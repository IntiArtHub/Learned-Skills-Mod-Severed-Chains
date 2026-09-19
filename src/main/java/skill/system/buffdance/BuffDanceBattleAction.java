package skill.system.buffdance;

import legend.core.gte.MV;
import legend.core.renderer.Obj;
import legend.core.renderer.PolyBuilder;
import legend.core.renderer.QueuedModelStandard;
import legend.core.renderer.Translucency;
import legend.game.combat.Battle;
import legend.game.combat.bent.PlayerBattleEntity;
import legend.game.combat.ui.BattleAction;
import legend.game.combat.ui.BattleActionTickFlowControl;
import legend.game.combat.ui.BattleActionUseFlowControl;
import legend.game.i18n.I18n;
import legend.game.inventory.screens.FontOptions;
import legend.game.inventory.screens.HorizontalAlign;
import legend.game.inventory.screens.TextColour;
import legend.game.ui.UiBox;
import org.joml.Vector3f;
import skill.system.SkillSystemRuntime;
import skill.system.battle.BattleSkillMastery;
import skill.system.battle.BridgedBattleAction;
import skill.system.equipment.ManualAtlasIcon;
import java.util.ArrayList;
import java.util.List;

import static legend.core.GameEngine.RENDERER;
import static legend.game.Scus94491BpeSegment_8006.battleState_8006e398;
import static legend.game.Text.renderText;
import static legend.game.Text.textZ_800bdf00;

/** A bounded, visual-only dance and beam sequence. The shared bridge owns turn completion. */
public final class BuffDanceBattleAction extends BattleAction implements BridgedBattleAction {
  private static final int DANCE_TICKS = 48;
  private static final int BEAM_TICKS = 36;
  private static final FontOptions MENU_FONT = new FontOptions().size(0.67f).colour(TextColour.WHITE)
    .shadowColour(TextColour.BLACK).horizontalAlign(HorizontalAlign.CENTRE);
  private static final FontOptions MESSAGE_FONT = new FontOptions().size(0.8f).colour(TextColour.WHITE)
    .shadowColour(TextColour.BLACK).horizontalAlign(HorizontalAlign.CENTRE);
  private static final UiBox MESSAGE_BOX = new UiBox(45, 20, 230, 16);
  private final Vector3f origin = new Vector3f();
  private final Vector3f rotation = new Vector3f();
  private final MV transforms = new MV();
  private final List<PlayerBattleEntity> targets = new ArrayList<>();
  private PlayerBattleEntity actor;
  private Obj beam;
  private int tick;
  private int rank;

  @Override
  public BattleActionUseFlowControl use(final Battle battle, final PlayerBattleEntity player) {
    this.reset();
    if(!SkillSystemRuntime.canUseSkill(player.character, SkillSystemRuntime.BUFF_DANCE)) {
      return BattleActionUseFlowControl.FAIL;
    }
    this.actor = player;
    this.origin.set(player.getPosition());
    this.rotation.set(player.getRotation());
    this.rank = SkillSystemRuntime.getSkillRank(player.character, SkillSystemRuntime.BUFF_DANCE);
    return BattleActionUseFlowControl.CONTINUE_SCRIPT;
  }

  @Override
  public BattleActionTickFlowControl tick(final Battle battle, final PlayerBattleEntity player) {
    if(this.actor != player) throw new IllegalStateException("Buff Dance ticked without its caster");
    this.tick++;
    if(this.tick <= DANCE_TICKS) {
      // Three small hops with alternating sway, preserving the character's own idle animation.
      final float phase = this.tick * (float)Math.PI / 8.0f;
      player.getPosition().set(this.origin).add((float)Math.sin(phase) * 65.0f,
        -(float)Math.abs(Math.sin(phase / 2.0f)) * 100.0f, 0.0f);
      player.getRotation().set(this.rotation).add(0, (float)Math.sin(phase) * 0.45f, (float)Math.sin(phase) * 0.10f);
      if(this.tick == DANCE_TICKS) {
        this.restoreActor();
        for(int slot = 0; slot < battleState_8006e398.getPlayerCount(); slot++) {
          final var state = battleState_8006e398.playerBents_e40.get(slot);
          if(state != null) {
            final PlayerBattleEntity target = state.innerStruct_00;
            this.targets.add(target);
            BuffDanceEffect.apply(target, player, this.rank);
          }
        }
        BattleSkillMastery.record(player, SkillSystemRuntime.BUFF_DANCE);
        this.beam = buildBeam();
      }
    } else {
      this.renderBeams();
    }
    this.renderMessage();
    if(this.tick < DANCE_TICKS + BEAM_TICKS) return BattleActionTickFlowControl.PAUSE_SCRIPT;
    this.reset();
    return BattleActionTickFlowControl.CONTINUE_SCRIPT;
  }

  private void restoreActor() {
    if(this.actor != null) {
      this.actor.getPosition().set(this.origin);
      this.actor.getRotation().set(this.rotation);
    }
  }

  public void reset() {
    this.restoreActor();
    this.actor = null;
    this.tick = 0;
    this.targets.clear();
    if(this.beam != null) { this.beam.delete(); this.beam = null; }
  }

  private static Obj buildBeam() {
    final PolyBuilder mesh = new PolyBuilder("Buff Dance blue light")
      .translucency(Translucency.B_PLUS_F).disableBackfaceCulling();
    for(int segment = 0; segment < 16; segment++) {
      final double a = segment * Math.PI / 8;
      final double b = (segment + 1) * Math.PI / 8;
      final float x1 = (float)Math.cos(a), z1 = (float)Math.sin(a);
      final float x2 = (float)Math.cos(b), z2 = (float)Math.sin(b);
      mesh.addVertex(x1, 0, z1).rgb(0.12f, 0.35f, 0.55f);
      mesh.addVertex(x2, 0, z2).rgb(0.12f, 0.35f, 0.55f);
      mesh.addVertex(x1, -1, z1).rgb(0, 0, 0);
      mesh.addVertex(x2, 0, z2).rgb(0.12f, 0.35f, 0.55f);
      mesh.addVertex(x2, -1, z2).rgb(0, 0, 0);
      mesh.addVertex(x1, -1, z1).rgb(0, 0, 0);
    }
    return mesh.build();
  }

  private void renderBeams() {
    final float progress = (this.tick - DANCE_TICKS) / (float)BEAM_TICKS;
    final float radius = 130.0f + 75.0f * (float)Math.sin(progress * Math.PI);
    final float height = 900.0f * (float)Math.sin(progress * Math.PI);
    for(final PlayerBattleEntity target : this.targets) {
      this.transforms.scaling(radius, height, radius);
      this.transforms.transfer.set(target.getPosition());
      RENDERER.queueModel(this.beam, this.transforms, QueuedModelStandard.class);
    }
  }

  private void renderMessage() {
    MESSAGE_BOX.render();
    final int oldZ = textZ_800bdf00;
    textZ_800bdf00 = 0;
    renderText(I18n.translate("skill_system.message.buff_dance", BuffDanceEffect.strength(this.rank)), 160, 23, MESSAGE_FONT);
    textZ_800bdf00 = oldZ;
  }

  @Override
  public void draw(final Battle battle, final int index, final boolean selected) {
    final var menu = battle.hud.battleMenu_800c6c34;
    final int x = menu.x_06 - menu.xShiftOffset_0a + index * 19;
    final int y = menu.y_08 - 16;
    menu.transforms.scaling(16.0f, 16.0f, 1.0f);
    menu.transforms.transfer.set(x, y, 123.8f);
    final int iconState = selected ? new int[] {0, 1, 2, 1}[menu.iconStateIndex_26] : 0;
    ManualAtlasIcon.renderBuffDanceAction(menu.transforms, iconState);
    final int oldZ = textZ_800bdf00;
    textZ_800bdf00 = 124;
    if(selected && menu.renderSelectedIconText_40) {
      renderText(I18n.translate("skill_system.battle_action.buff_dance.name"), x + 8, y - 8, MENU_FONT);
    }
    textZ_800bdf00 = oldZ;
  }
}
