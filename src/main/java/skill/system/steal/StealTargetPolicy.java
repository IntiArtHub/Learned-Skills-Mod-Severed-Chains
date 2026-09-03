package skill.system.steal;

import java.util.List;

/** Pure boss-target rules shared by battle-menu disabling and live confirmation. */
public final class StealTargetPolicy {
  private StealTargetPolicy() { }

  public static boolean canTarget(final int rank, final boolean boss) {
    return rank >= 3 || !boss;
  }

  public static boolean shouldDisableAction(final int rank, final List<Boolean> livingTargetBossFlags) {
    return !livingTargetBossFlags.isEmpty() && livingTargetBossFlags.stream().noneMatch(boss -> canTarget(rank, boss));
  }
}
