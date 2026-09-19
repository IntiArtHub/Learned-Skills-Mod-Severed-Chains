package skill.system.quickchange;

import legend.game.combat.Battle;
import legend.game.combat.bent.PlayerBattleEntity;
import legend.game.combat.ui.BattleAction;
import legend.game.combat.ui.BattleActionTickFlowControl;
import legend.game.combat.ui.BattleActionUseFlowControl;
import legend.game.combat.ui.ListPosition;
import legend.game.inventory.Equipment;
import legend.game.inventory.screens.FontOptions;
import legend.game.inventory.screens.HorizontalAlign;
import legend.game.inventory.screens.TextColour;
import legend.game.i18n.I18n;
import skill.system.SkillSystemRuntime;
import skill.system.battle.BridgedBattleAction;
import skill.system.battle.BattleSkillMastery;
import skill.system.equipment.ManualAtlasIcon;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static legend.game.Text.renderText;
import static legend.game.Text.textZ_800bdf00;
import static legend.game.combat.bent.BattleEntity27c.FLAG_ANIMATE_ONCE;

/** One confirmed loadout per activation; the shared bridge owns turn completion. */
public final class QuickchangeBattleAction extends BattleAction implements BridgedBattleAction {
  private static final int FIRST_ANIMATION = 3;
  private static final int FINISH_ANIMATION = 31;
  private static final int FIRST_ANIMATION_TIMEOUT = 90;
  private static final int FINISH_ANIMATION_TICKS = 8;
  private static final FontOptions LABEL = new FontOptions().size(0.55f).colour(TextColour.WHITE)
    .shadowColour(TextColour.BLACK).horizontalAlign(HorizontalAlign.CENTRE);

  private final ListPosition position = new ListPosition();
  private final Map<String, Integer> usesThisBattle = new HashMap<>();
  private PlayerBattleEntity actor;
  private boolean committed;
  private boolean usedThroughManual;
  private int originalAnimation;
  private boolean originalAnimateOnce;
  /** 0 = not started, 1 = animation 3, 2 = brief animation 31. */
  private int animationStage;
  private int completionTick;

  public void reset() {
    this.finishAction();
    this.usesThisBattle.clear();
  }

  private void finishAction() {
    this.actor = null;
    this.committed = false;
    this.usedThroughManual = false;
    this.animationStage = 0;
    this.completionTick = 0;
  }

  public boolean canUse(final PlayerBattleEntity player) {
    if(!SkillSystemRuntime.canUseSkill(player.character, SkillSystemRuntime.QUICKCHANGE)) return false;
    final int rank = SkillSystemRuntime.getSkillRank(player.character, SkillSystemRuntime.QUICKCHANGE);
    if(rank < 4 && this.usesThisBattle.getOrDefault(SkillSystemRuntime.characterId(player.character), 0) >= rank) return false;
    return !QuickchangeEquipmentScope.candidates(player).isEmpty();
  }

  @Override
  public BattleActionUseFlowControl use(final Battle battle, final PlayerBattleEntity player) {
    if(!this.canUse(player)) return BattleActionUseFlowControl.FAIL;
    this.actor = player;
    this.committed = false;
    this.usedThroughManual = SkillSystemRuntime.isSkillManualEquipped(player.character, SkillSystemRuntime.QUICKCHANGE);
    this.originalAnimation = player.loadingAnimIndex_26e;
    this.originalAnimateOnce = player.getState().hasFlag(FLAG_ANIMATE_ONCE);
    this.animationStage = 0;
    this.completionTick = 0;
    battle.hud.listMenu_800c6b60 = new QuickchangeEquipmentMenu(
      battle.hud, player, this.position,
      changes -> {
        if(QuickchangeEquipmentTransaction.commitAll(player, changes)) {
          this.committed = true;
          this.usesThisBattle.merge(SkillSystemRuntime.characterId(player.character), 1, Integer::sum);
          BattleSkillMastery.recordQuickchange(player, this.usedThroughManual);
          return true;
        }
        return false;
      },
      () -> battle.hud.listMenu_800c6b60 = null
    );
    return BattleActionUseFlowControl.CONTINUE_SCRIPT;
  }

  @Override
  public BattleActionTickFlowControl tick(final Battle battle, final PlayerBattleEntity player) {
    if(this.actor != player) return BattleActionTickFlowControl.REPEAT_TURN;
    if(!this.committed) {
      this.finishAction();
      return BattleActionTickFlowControl.REPEAT_TURN;
    }
    if(this.animationStage == 0) {
      this.animationStage = this.playAnimation(battle, player, FIRST_ANIMATION, false) ? 1 : 2;
      this.completionTick = 0;
      if(this.animationStage == 1) return BattleActionTickFlowControl.PAUSE_SCRIPT;
    }
    if(this.animationStage == 1) {
      this.completionTick++;
      if(player.model_148.remainingFrames_9e > 0 && this.completionTick < FIRST_ANIMATION_TIMEOUT) {
        return BattleActionTickFlowControl.PAUSE_SCRIPT;
      }
      this.animationStage = 2;
      this.completionTick = 0;
      if(!this.playAnimation(battle, player, FINISH_ANIMATION, false)) return this.complete(battle, player);
      this.completionTick = 1;
      return BattleActionTickFlowControl.PAUSE_SCRIPT;
    } else if(this.animationStage == 2 && this.completionTick == 0) {
      if(this.playAnimation(battle, player, FINISH_ANIMATION, false)) {
        this.completionTick = 1;
        return BattleActionTickFlowControl.PAUSE_SCRIPT;
      }
      return this.complete(battle, player);
    }
    if(this.animationStage == 2 && ++this.completionTick < FINISH_ANIMATION_TICKS) {
      return BattleActionTickFlowControl.PAUSE_SCRIPT;
    }
    return this.complete(battle, player);
  }

  private BattleActionTickFlowControl complete(final Battle battle, final PlayerBattleEntity player) {
    this.playAnimation(battle, player, this.originalAnimation, !this.originalAnimateOnce);
    this.finishAction();
    return BattleActionTickFlowControl.CONTINUE_SCRIPT;
  }

  private boolean playAnimation(final Battle battle, final PlayerBattleEntity player,
                                final int animation, final boolean loop) {
    if(animation < 0 || animation >= player.combatant_144.assets_14.length ||
      player.combatant_144.assets_14[animation] == null) return false;
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

  @Override
  public void draw(final Battle battle, final int index, final boolean selected) {
    final var menu = battle.hud.battleMenu_800c6c34;
    final int x = menu.x_06 - menu.xShiftOffset_0a + index * 19 + 8;
    final int y = menu.y_08 - 16;
    final int previousZ = textZ_800bdf00;
    menu.transforms.scaling(16.0f, 16.0f, 1.0f);
    menu.transforms.transfer.set(x - 8, y, 123.8f);
    final int iconFrame = selected ? new int[] {0, 1, 2, 1}[menu.iconStateIndex_26] : 0;
    ManualAtlasIcon.renderQuickchangeAction(menu.transforms, iconFrame);
    textZ_800bdf00 = 124;
    if(selected && menu.renderSelectedIconText_40) renderText(I18n.translate("skill_system.battle_action.quickchange.name"), x, y - 8, LABEL);
    textZ_800bdf00 = previousZ;
  }
}
