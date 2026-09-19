package skill.system.battle;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

final class BattleSkillUseLedgerTest {
  @Test void repeatedUsesAwardOnceOnlyToLivingUsersAndFinishCannotAwardTwice() {
    final var ledger = new BattleSkillUseLedger();
    final List<String> awards = new ArrayList<>();
    ledger.record("dart", "dance"); ledger.record("dart", "dance");
    ledger.record("rose", "dance");
    ledger.finish(true, id -> id.equals("dart"), (id, skill) -> awards.add(id + ':' + skill));
    ledger.finish(true, id -> true, (id, skill) -> awards.add(id + ':' + skill));
    assertEquals(List.of("dart:dance"), awards);
  }

  @Test void escapeDefeatAndAbortedBattlesDoNotLeakProgressToNextVictory() {
    final var ledger = new BattleSkillUseLedger();
    ledger.record("dart", "dance");
    ledger.finish(false, id -> true, (id, skill) -> fail("Non-victory credit"));
    ledger.finish(true, id -> true, (id, skill) -> fail("Leaked credit"));
    ledger.record("dart", "dance"); ledger.clear();
    ledger.finish(true, id -> true, (id, skill) -> fail("Aborted battle credit"));
    ledger.record("dart", "dance");
    final List<String> awards = new ArrayList<>();
    ledger.finish(true, id -> true, (id, skill) -> awards.add(id));
    assertEquals(List.of("dart"), awards);
  }
}
