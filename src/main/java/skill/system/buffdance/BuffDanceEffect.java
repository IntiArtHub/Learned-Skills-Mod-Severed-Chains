package skill.system.buffdance;

import legend.game.characters.UnaryStat;
import legend.game.combat.bent.PlayerBattleEntity;
import org.legendofdragoon.modloader.registries.RegistryId;
import java.util.List;

import static legend.lodmod.LodMod.*;

public final class BuffDanceEffect {
  public static final RegistryId MOD_ID = new RegistryId("skill_system", "buff_dance");

  private BuffDanceEffect() { }

  public static int strength(final int rank) {
    return switch(rank) {
      case 1 -> 15;
      case 2 -> 30;
      case 3 -> 50;
      case 4 -> 75;
      default -> throw new IllegalArgumentException("Invalid Buff Dance rank " + rank);
    };
  }

  public static void apply(final PlayerBattleEntity target, final PlayerBattleEntity caster, final int rank) {
    for(final var type : List.of(ATTACK_STAT.get(), MAGIC_ATTACK_STAT.get(), DEFENSE_STAT.get(),
                                MAGIC_DEFENSE_STAT.get(), SPEED_STAT.get())) {
      refresh(target.stats.getStat(type), strength(rank), target == caster ? 4 : 3);
    }
    target.recalculateSpeedAndPerHitStats();
  }

  static void refresh(final UnaryStat stat, final int percentage, final int turns) {
    final var previous = stat.getMod(MOD_ID);
    final int strength = previous instanceof DanceStatMod dance ? Math.max(percentage, dance.percentage()) : percentage;
    stat.addMod(MOD_ID, new DanceStatMod(strength, turns));
  }
}
