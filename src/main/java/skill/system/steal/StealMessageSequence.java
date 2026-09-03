package skill.system.steal;

import skill.system.api.MasteryChange;

import java.util.ArrayList;
import java.util.List;

/** Pure ordered mastery notifications; translation is applied by the live action. */
public final class StealMessageSequence {
  public enum Message { LEVEL_2, LEVEL_3, MASTERED, MANUAL_CONSUMED }

  private StealMessageSequence() { }

  public static List<Message> forMasteryChange(final MasteryChange change, final boolean manualWasEquipped) {
    if(change == null || change.newRank() == change.oldRank()) return List.of();
    final List<Message> messages = new ArrayList<>();
    if(change.newRank() == 2) messages.add(Message.LEVEL_2);
    if(change.newRank() == 3) messages.add(Message.LEVEL_3);
    if(change.newlyMastered()) {
      messages.add(Message.MASTERED);
      if(manualWasEquipped) messages.add(Message.MANUAL_CONSUMED);
    }
    return List.copyOf(messages);
  }
}
