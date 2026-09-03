package skill.system.battle;

import legend.game.combat.ui.BattleAction;
import legend.game.combat.ui.BattleActionTickFlowControl;
import legend.game.combat.ui.BattleActionUseFlowControl;
import legend.game.combat.ui.BattleMenuStruct58;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards the stock 1739 generic custom-action lifecycle that replaced the local function-160 bridge. */
final class CurrentUpstreamBattleActionContractTest {
  @Test void battleActionUseHasTypedFlowControl() throws Exception {
    final Method method = BattleAction.class.getMethod("use", legend.game.combat.Battle.class, legend.game.combat.bent.PlayerBattleEntity.class);
    assertEquals(BattleActionUseFlowControl.class, method.getReturnType());
  }

  @Test void battleActionTickHasTypedFlowControl() throws Exception {
    final Method method = BattleAction.class.getMethod("tick", legend.game.combat.Battle.class, legend.game.combat.bent.PlayerBattleEntity.class);
    assertEquals(BattleActionTickFlowControl.class, method.getReturnType());
  }

  @Test void tickContractSupportsPersistenceCompletionAndCancellation() {
    assertEquals(Set.of("PAUSE_SCRIPT", "CONTINUE_SCRIPT", "IGNORE", "REPEAT_TURN"),
      Set.of(BattleActionTickFlowControl.values()).stream().map(Enum::name).collect(java.util.stream.Collectors.toSet()));
  }

  @Test void battleMenuRetainsCurrentActionAcrossTicks() throws Exception {
    assertEquals(BattleAction.class, BattleMenuStruct58.class.getField("currentAction").getType());
    assertTrue(java.lang.reflect.Modifier.isPublic(BattleMenuStruct58.class.getField("currentAction").getModifiers()));
  }
}