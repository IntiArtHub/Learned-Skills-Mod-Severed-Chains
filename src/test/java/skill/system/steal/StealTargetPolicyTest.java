package skill.system.steal;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class StealTargetPolicyTest {
  @Test void lowerRanksRejectBossButKeepNormalTargetsLegal() {
    assertFalse(StealTargetPolicy.canTarget(1, true));
    assertFalse(StealTargetPolicy.canTarget(2, true));
    assertTrue(StealTargetPolicy.canTarget(1, false));
    assertFalse(StealTargetPolicy.shouldDisableAction(2, List.of(true, false)));
  }

  @Test void allBossEncounterDisablesOnlyBelowRankThree() {
    assertTrue(StealTargetPolicy.shouldDisableAction(1, List.of(true, true)));
    assertTrue(StealTargetPolicy.shouldDisableAction(2, List.of(true)));
    assertFalse(StealTargetPolicy.shouldDisableAction(3, List.of(true, true)));
    assertTrue(StealTargetPolicy.canTarget(3, true));
  }

  @Test void noLivingTargetsDoesNotInventAProhibition() {
    assertFalse(StealTargetPolicy.shouldDisableAction(1, List.of()));
  }
}
