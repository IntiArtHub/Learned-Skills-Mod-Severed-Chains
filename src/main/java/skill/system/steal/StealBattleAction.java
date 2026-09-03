package skill.system.steal;

import legend.game.combat.Battle;
import legend.game.combat.bent.MonsterBattleEntity;
import legend.game.combat.bent.PlayerBattleEntity;
import legend.game.combat.ui.BattleAction;
import legend.game.combat.ui.BattleActionTickFlowControl;
import legend.game.combat.ui.BattleActionUseFlowControl;
import legend.game.i18n.I18n;
import legend.game.inventory.InventoryEntry;
import legend.game.inventory.screens.FontOptions;
import legend.game.inventory.screens.HorizontalAlign;
import legend.game.inventory.screens.TextColour;
import legend.game.scripting.ScriptState;
import legend.game.ui.UiBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Vector3f;
import skill.system.SkillSystemDiagnostics;
import skill.system.SkillSystemRuntime;
import skill.system.api.MasteryChange;
import skill.system.battle.BridgedBattleAction;
import skill.system.equipment.ManualAtlasIcon;

import java.util.ArrayDeque;
import java.util.Deque;

import static legend.core.GameEngine.PLATFORM;
import static legend.core.GameEngine.SCRIPTS;
import static legend.game.Text.renderText;
import static legend.game.Text.textZ_800bdf00;
import static legend.game.combat.bent.BattleEntity27c.FLAG_ANIMATE_ONCE;
import static legend.game.modding.coremod.CoreMod.INPUT_ACTION_MENU_CONFIRM;

/** Initial safe, player-owned Steal implementation. Visual fidelity requires human verification. */
public final class StealBattleAction extends BattleAction implements BridgedBattleAction {
  private static final Logger LOGGER = LogManager.getFormatterLogger(StealBattleAction.class);
  private static final int APPROACH_TICKS = 15;
  private static final int RETURN_TICKS = 3;
  private static final int CONTACT_TIMEOUT_TICKS = 60;
  private static final int MESSAGE_TICKS = 50;
  private static final float CONTACT_DISTANCE = 350.0f;

  private static final FontOptions MENU_FONT = new FontOptions().size(0.67f).colour(TextColour.WHITE)
    .shadowColour(TextColour.BLACK).horizontalAlign(HorizontalAlign.CENTRE);
  private static final FontOptions MESSAGE_FONT = new FontOptions().size(0.8f).colour(TextColour.WHITE)
    .shadowColour(TextColour.BLACK).horizontalAlign(HorizontalAlign.CENTRE);
  private static final UiBox MESSAGE_BOX = createMessageBox();

  enum Stage { IDLE, TARGET_WARMUP, TARGETING, INVALID_TARGET, APPROACH, CONTACT, RESULT_MESSAGE, RETURN, MASTERY_MESSAGES }

  private final StealExecutionService execution = new StealExecutionService();
  private final Deque<String> masteryMessages = new ArrayDeque<>();
  private final Vector3f origin = new Vector3f();
  private final Vector3f approach = new Vector3f();

  private Stage stage = Stage.IDLE;
  private int stageTick;
  private String message;
  private MonsterBattleEntity target;
  private float originRotationY;
  private int originAnimation;
  private boolean originAnimateOnce;

  @Override
  public BattleActionUseFlowControl use(final Battle battle, final PlayerBattleEntity player) {
    this.reset();
    this.stage = Stage.TARGET_WARMUP;
    SkillSystemDiagnostics.log(LOGGER, "Steal selected actor=%s mastery=%d rank=%d",
      player.getName(), SkillSystemRuntime.getSkillMastery(player.character, SkillSystemRuntime.STEAL),
      SkillSystemRuntime.getSkillRank(player.character, SkillSystemRuntime.STEAL));
    return BattleActionUseFlowControl.CONTINUE_SCRIPT;
  }

  @Override
  public BattleActionTickFlowControl tick(final Battle battle, final PlayerBattleEntity player) {
    return switch(this.stage) {
      case IDLE -> throw new IllegalStateException("Steal ticked before use");
      case TARGET_WARMUP -> {
        this.stage = Stage.TARGETING;
        yield BattleActionTickFlowControl.PAUSE_SCRIPT;
      }
      case TARGETING -> this.tickTargeting(battle, player);
      case INVALID_TARGET -> this.tickInvalidTarget();
      case APPROACH -> this.tickApproach(battle, player);
      case CONTACT -> this.tickContact(battle, player);
      case RESULT_MESSAGE -> this.tickResultMessage(battle, player);
      case RETURN -> this.tickReturn(battle, player);
      case MASTERY_MESSAGES -> this.tickMasteryMessages();
    };
  }

  private BattleActionTickFlowControl tickTargeting(final Battle battle, final PlayerBattleEntity player) {
    final int targetFlow = battle.hud.handleTargeting(1, false);
    if(targetFlow == 0) return BattleActionTickFlowControl.PAUSE_SCRIPT;
    if(targetFlow < 0) {
      SkillSystemDiagnostics.log(LOGGER, "Steal targeting cancelled actor=%s", player.getName());
      this.reset();
      return BattleActionTickFlowControl.REPEAT_TURN;
    }

    final int targetIndex = battle.hud.battleMenu_800c6c34.target_48;
    final MonsterBattleEntity selected = SCRIPTS.getObject(targetIndex, MonsterBattleEntity.class);
    final int rank = SkillSystemRuntime.getSkillRank(player.character, SkillSystemRuntime.STEAL);
    final boolean boss = StealBossRegistry.isBoss(selected.charId_272);
    SkillSystemDiagnostics.log(LOGGER, "Steal target actor=%s target=%s charId=%d boss=%s rank=%d",
      player.getName(), selected.getName(), selected.charId_272, boss, rank);

    if(!StealTargetPolicy.canTarget(rank, boss)) {
      this.message = I18n.translate("skill_system.message.cant_steal_boss");
      this.stage = Stage.INVALID_TARGET;
      this.stageTick = 0;
      return BattleActionTickFlowControl.PAUSE_SCRIPT;
    }

    this.target = selected;
    this.origin.set(player.getPosition());
    this.originRotationY = player.getRotation().y;
    this.originAnimation = player.loadingAnimIndex_26e;
    this.originAnimateOnce = player.getState().hasFlag(FLAG_ANIMATE_ONCE);
    this.calculateApproach(player, selected);
    this.ensureAnimation(battle, player, 2, true);
    this.face(player, selected.getPosition());
    this.stage = Stage.APPROACH;
    this.stageTick = 0;
    return BattleActionTickFlowControl.PAUSE_SCRIPT;
  }

  private BattleActionTickFlowControl tickInvalidTarget() {
    this.renderMessage(this.message);
    this.stageTick++;
    if(this.stageTick >= MESSAGE_TICKS || (this.stageTick >= 8 && PLATFORM.isActionPressed(INPUT_ACTION_MENU_CONFIRM.get()))) {
      this.stage = Stage.TARGET_WARMUP;
      this.stageTick = 0;
      this.message = null;
    }
    return BattleActionTickFlowControl.PAUSE_SCRIPT;
  }

  private BattleActionTickFlowControl tickApproach(final Battle battle, final PlayerBattleEntity player) {
    this.stageTick++;
    this.lerpPosition(player, this.origin, this.approach, this.stageTick, APPROACH_TICKS);
    if(this.stageTick >= APPROACH_TICKS) {
      player.getPosition().set(this.approach);
      final int contactAnimation = this.ensureContactAnimation(battle, player);
      SkillSystemDiagnostics.log(LOGGER, "Steal contact animation=%d frames=%d oneShot=%s",
        contactAnimation, player.model_148.remainingFrames_9e, player.getState().hasFlag(FLAG_ANIMATE_ONCE));
      this.stage = Stage.CONTACT;
      this.stageTick = 0;
    }
    return BattleActionTickFlowControl.PAUSE_SCRIPT;
  }

  private BattleActionTickFlowControl tickContact(final Battle battle, final PlayerBattleEntity player) {
    this.stageTick++;
    if(player.model_148.remainingFrames_9e > 0 && this.stageTick < CONTACT_TIMEOUT_TICKS) {
      return BattleActionTickFlowControl.PAUSE_SCRIPT;
    }

    final boolean manualWasEquipped = SkillSystemRuntime.isSkillManualEquipped(player.character, SkillSystemRuntime.STEAL);
    final int resolutionRank = SkillSystemRuntime.getSkillRank(player.character, SkillSystemRuntime.STEAL);
    final StealExecutionService.Result result = this.execution.execute(player, this.target);
    this.message = this.resultMessage(result);
    this.queueMasteryMessages(player, result.masteryChange(), manualWasEquipped);
    SkillSystemDiagnostics.log(LOGGER,
      "Steal resolution target=%s resource=%s baseChance=%d multiplier=%d finalChance=%d roll=%d outcome=%s mastery=%s",
      this.target.getName(), result.resource(), result.resolution().baseChance(),
      resolutionRank, result.resolution().finalChance(),
      result.resolution().roll(), result.outcome(), result.masteryChange());
    this.stage = Stage.RESULT_MESSAGE;
    this.stageTick = 0;
    return BattleActionTickFlowControl.PAUSE_SCRIPT;
  }

  private BattleActionTickFlowControl tickResultMessage(final Battle battle, final PlayerBattleEntity player) {
    this.renderMessage(this.message);
    this.stageTick++;
    if(this.stageTick >= MESSAGE_TICKS || (this.stageTick >= 8 && PLATFORM.isActionPressed(INPUT_ACTION_MENU_CONFIRM.get()))) {
      this.face(player, this.origin);
      this.ensureAnimation(battle, player, 2, true);
      this.stage = Stage.RETURN;
      this.stageTick = 0;
    }
    return BattleActionTickFlowControl.PAUSE_SCRIPT;
  }

  private BattleActionTickFlowControl tickReturn(final Battle battle, final PlayerBattleEntity player) {
    this.stageTick++;
    this.lerpPosition(player, this.approach, this.origin, this.stageTick, RETURN_TICKS);
    if(this.stageTick < RETURN_TICKS) return BattleActionTickFlowControl.PAUSE_SCRIPT;

    player.getPosition().set(this.origin);
    player.getRotation().y = this.originRotationY;
    this.ensureAnimation(battle, player, this.originAnimation, !this.originAnimateOnce);
    if(!this.masteryMessages.isEmpty()) {
      this.message = this.masteryMessages.removeFirst();
      this.stage = Stage.MASTERY_MESSAGES;
      this.stageTick = 0;
      return BattleActionTickFlowControl.PAUSE_SCRIPT;
    }

    SkillSystemDiagnostics.log(LOGGER, "Steal complete actor=%s positionRestored=%s", player.getName(), player.getPosition());
    this.reset();
    return BattleActionTickFlowControl.CONTINUE_SCRIPT;
  }

  private BattleActionTickFlowControl tickMasteryMessages() {
    this.renderMessage(this.message);
    this.stageTick++;
    if(this.stageTick < MESSAGE_TICKS && (this.stageTick < 8 || !PLATFORM.isActionPressed(INPUT_ACTION_MENU_CONFIRM.get()))) {
      return BattleActionTickFlowControl.PAUSE_SCRIPT;
    }

    if(!this.masteryMessages.isEmpty()) {
      this.message = this.masteryMessages.removeFirst();
      this.stageTick = 0;
      return BattleActionTickFlowControl.PAUSE_SCRIPT;
    }

    SkillSystemDiagnostics.log(LOGGER, "Steal complete after mastery messages");
    this.reset();
    return BattleActionTickFlowControl.CONTINUE_SCRIPT;
  }

  private void calculateApproach(final PlayerBattleEntity player, final MonsterBattleEntity enemy) {
    final Vector3f away = new Vector3f(player.getPosition()).sub(enemy.getPosition());
    away.y = 0.0f;
    if(away.lengthSquared() < 1.0f) away.set(1.0f, 0.0f, 0.0f);
    away.normalize(CONTACT_DISTANCE);
    this.approach.set(enemy.getPosition()).add(away);
    this.approach.y = this.origin.y;
  }

  private void face(final PlayerBattleEntity player, final Vector3f point) {
    player.getRotation().y = (float)Math.atan2(point.x - player.getPosition().x, point.z - player.getPosition().z) + (float)Math.PI;
  }

  private void lerpPosition(final PlayerBattleEntity player, final Vector3f from, final Vector3f to, final int tick, final int ticks) {
    player.getPosition().set(from).lerp(to, Math.min(1.0f, tick / (float)ticks));
  }

  private boolean ensureAnimation(final Battle battle, final PlayerBattleEntity player, final int animation,
                                  final boolean loop) {
    if(animation < 0 || animation >= player.combatant_144.assets_14.length || player.combatant_144.assets_14[animation] == null) return false;
    battle.FUN_800c9e10(player.combatant_144, animation);
    if(!player.combatant_144.isAssetLoaded(animation)) return false;
    final int previous = player.loadingAnimIndex_26e;
    if(previous >= 0 && previous < player.combatant_144.assets_14.length && previous != animation) {
      Battle.FUN_800ca194(player.combatant_144.assets_14[previous]);
    }
    battle.loadAnimationAssetIntoModel(player.model_148, player.combatant_144, animation);
    player.loadingAnimIndex_26e = animation;
    player.currentAnimIndex_270 = -1;
    player.model_148.animationState_9c = 1;
    if(loop) player.getState().clearFlag(FLAG_ANIMATE_ONCE);
    else player.getState().setFlag(FLAG_ANIMATE_ONCE);
    return true;
  }

  private int ensureContactAnimation(final Battle battle, final PlayerBattleEntity player) {
    // Animation 7 is the retail player throw/item-use motion. Play it once; 8 is attack-ready fallback.
    if(this.ensureAnimation(battle, player, 7, false)) return 7;
    if(this.ensureAnimation(battle, player, 8, false)) return 8;
    return -1;
  }

  private String resultMessage(final StealExecutionService.Result result) {
    return switch(result.outcome()) {
      case NOTHING -> I18n.translate("skill_system.message.nothing_to_steal");
      case FAILED -> I18n.translate("skill_system.message.failed_to_steal");
      case INVENTORY_FULL -> I18n.translate("skill_system.message.items_full");
      case SUCCESS -> I18n.translate("skill_system.message.stole", this.resourceName(result.resource()));
    };
  }

  private String resourceName(final HeldResource resource) {
    if(resource == null) return "?";
    if(resource.type() == HeldResource.ResourceType.GOLD) return resource.value() + "G";
    if(resource.value() instanceof InventoryEntry<?> entry) {
      return I18n.translate(entry.getNameTranslationKey());
    }
    return String.valueOf(resource.value());
  }

  private void queueMasteryMessages(final PlayerBattleEntity player, final MasteryChange change, final boolean manualWasEquipped) {
    for(final StealMessageSequence.Message message : StealMessageSequence.forMasteryChange(change, manualWasEquipped)) {
      this.masteryMessages.add(switch(message) {
        case LEVEL_2 -> I18n.translate("skill_system.message.steal_level_2", player.getName());
        case LEVEL_3 -> I18n.translate("skill_system.message.steal_level_3", player.getName());
        case MASTERED -> I18n.translate("skill_system.message.steal_mastered", player.getName());
        case MANUAL_CONSUMED -> I18n.translate("skill_system.message.manual_consumed");
      });
    }
  }

  private void renderMessage(final String text) {
    final int width = Math.clamp((int)Math.ceil(legend.core.GameEngine.DEFAULT_FONT.lineWidth(text)
      * MESSAGE_FONT.getSize()) + 20, 120, 280);
    MESSAGE_BOX.setPos((320 - width) / 2, 20);
    MESSAGE_BOX.setSize(width, 16);
    MESSAGE_BOX.render();
    final int oldZ = textZ_800bdf00;
    textZ_800bdf00 = 0;
    renderText(text, 160.0f, 23.0f, MESSAGE_FONT);
    textZ_800bdf00 = oldZ;
  }

  private void reset() {
    this.stage = Stage.IDLE;
    this.stageTick = 0;
    this.message = null;
    this.target = null;
    this.masteryMessages.clear();
  }

  @Override
  public void draw(final Battle battle, final int index, final boolean selected) {
    final var menu = battle.hud.battleMenu_800c6c34;
    final int x = menu.x_06 - menu.xShiftOffset_0a + index * 19;
    final int y = menu.y_08 - 16;
    menu.transforms.scaling(16.0f, 16.0f, 1.0f);
    menu.transforms.transfer.set(x, y, 123.8f);
    ManualAtlasIcon.STEAL.renderBattle(menu.transforms);
    final int oldZ = textZ_800bdf00;
    textZ_800bdf00 = 124;
    if(selected && menu.renderSelectedIconText_40) {
      renderText(I18n.translate("skill_system.battle_action.steal.name"), x + 8, y - 8, MENU_FONT);
    }
    textZ_800bdf00 = oldZ;
  }

  private static UiBox createMessageBox() {
    return new UiBox(60, 20, 200, 16);
  }
}
