package skill.system.steal;

import org.junit.jupiter.api.Test;
import skill.system.api.MasteryChange;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class StealMessageSequenceTest {
  @Test void rankMessagesAreEmittedAtTheirThresholds() {
    assertEquals(List.of(StealMessageSequence.Message.LEVEL_2),
      StealMessageSequence.forMasteryChange(new MasteryChange(19, 20, 1, 2, false), true));
    assertEquals(List.of(StealMessageSequence.Message.LEVEL_3),
      StealMessageSequence.forMasteryChange(new MasteryChange(59, 60, 2, 3, false), true));
  }

  @Test void masteryThenConsumptionOrderingIsStable() {
    assertEquals(List.of(StealMessageSequence.Message.MASTERED, StealMessageSequence.Message.MANUAL_CONSUMED),
      StealMessageSequence.forMasteryChange(new MasteryChange(98, 99, 3, 4, true), true));
    assertEquals(List.of(StealMessageSequence.Message.MASTERED),
      StealMessageSequence.forMasteryChange(new MasteryChange(98, 99, 3, 4, true), false));
  }

  @Test void ordinarySuccessProducesNoRankMessage() {
    assertEquals(List.of(), StealMessageSequence.forMasteryChange(new MasteryChange(12, 13, 1, 1, false), true));
  }
}
