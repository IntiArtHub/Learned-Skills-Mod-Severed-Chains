package skill.system.steal;

import legend.game.combat.bent.MonsterBattleEntity;
import legend.game.combat.bent.PlayerBattleEntity;
import legend.game.inventory.Equipment;
import legend.game.inventory.ItemStack;
import skill.system.SkillSystemMod;
import skill.system.SkillSystemDiagnostics;
import skill.system.SkillSystemRuntime;
import skill.system.api.MasteryChange;

import static legend.game.SItem.addGold;
import static legend.game.SItem.giveEquipment;
import static legend.game.Scus94491BpeSegment_800b.gameState_800babc8;

/** Category-correct reward mutation plus the Steal-specific successful-acquisition mastery trigger. */
public final class StealExecutionService {
  private final StealResolver resolver = new StealResolver();

  public Result execute(final PlayerBattleEntity player, final MonsterBattleEntity enemy) {
    final HeldResourceState state = SkillSystemMod.HELD_RESOURCES.get(enemy);
    if(state == null) return new Result(new StealResolver.Resolution(StealResolver.Outcome.NOTHING, 0, 0, -1), null, null);
    final int rank = SkillSystemRuntime.getSkillRank(player.character, SkillSystemRuntime.STEAL);
    final HeldResource resource = state.resource();
    final StealResolver.Resolution resolution = this.resolver.resolveDetailed(state, rank, SkillSystemMod.battleRng(),
      this::award, SkillSystemDiagnostics.forceStealSuccess());
    final MasteryChange mastery = resolution.outcome() == StealResolver.Outcome.SUCCESS
      ? SkillSystemRuntime.addSkillMastery(player.character, SkillSystemRuntime.STEAL, 1)
      : null;
    return new Result(resolution, mastery, resource);
  }

  private boolean award(final HeldResource resource) {
    return switch(resource.type()) {
      case ITEM -> gameState_800babc8.items_2e9.give(new ItemStack((ItemStack)resource.value())).isEmpty();
      case EQUIPMENT -> giveEquipment((Equipment)resource.value());
      case GOLD -> { addGold((Integer)resource.value()); yield true; }
    };
  }

  public record Result(StealResolver.Resolution resolution, MasteryChange masteryChange, HeldResource resource) {
    public StealResolver.Outcome outcome() { return this.resolution.outcome(); }
  }
}
