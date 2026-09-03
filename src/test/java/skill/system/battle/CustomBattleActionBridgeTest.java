package skill.system.battle;

import legend.game.combat.Battle;
import legend.game.combat.ui.BattleAction;
import legend.game.scripting.FlowControl;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class CustomBattleActionBridgeTest {
  @Test void nativeSelectionUsesRetailResultUnchanged() {
    final AtomicReference<BattleAction> action = new AtomicReference<>();
    final AtomicInteger calls = new AtomicInteger();
    final CustomBattleActionBridge bridge = new CustomBattleActionBridge(action::get, ignored -> {
      calls.incrementAndGet();
      action.set(new TestNativeAction());
      return FlowControl.CONTINUE;
    });

    assertEquals(FlowControl.CONTINUE, bridge.apply(null));
    assertEquals(1, calls.get());
  }

  @Test void customSelectionRewindsThenPausedTicksAndCompletionPassThrough() {
    final AtomicReference<BattleAction> action = new AtomicReference<>();
    final AtomicInteger calls = new AtomicInteger();
    final BattleAction custom = new TestCustomAction();
    final CustomBattleActionBridge bridge = new CustomBattleActionBridge(action::get, ignored -> {
      return switch(calls.getAndIncrement()) {
        case 0 -> { action.set(custom); yield FlowControl.CONTINUE; }
        case 1, 2 -> FlowControl.PAUSE_AND_REWIND;
        default -> FlowControl.CONTINUE;
      };
    });

    assertEquals(FlowControl.PAUSE_AND_REWIND, bridge.apply(null));
    assertEquals(FlowControl.PAUSE_AND_REWIND, bridge.apply(null));
    assertEquals(FlowControl.PAUSE_AND_REWIND, bridge.apply(null));
    assertEquals(FlowControl.CONTINUE, bridge.apply(null));
    assertEquals(4, calls.get());
  }

  @Test void customCancellationReturnsToMenuWithoutCompletingScript() {
    final AtomicReference<BattleAction> action = new AtomicReference<>();
    final AtomicInteger calls = new AtomicInteger();
    final CustomBattleActionBridge bridge = new CustomBattleActionBridge(action::get, ignored -> {
      if(calls.getAndIncrement() == 0) {
        action.set(new TestCustomAction());
        return FlowControl.CONTINUE;
      }
      action.set(null);
      return FlowControl.PAUSE_AND_REWIND;
    });

    assertEquals(FlowControl.PAUSE_AND_REWIND, bridge.apply(null));
    assertEquals(FlowControl.PAUSE_AND_REWIND, bridge.apply(null));
    assertEquals(2, calls.get());
  }

  private static final class TestNativeAction extends BattleAction {
    @Override public void draw(final Battle battle, final int index, final boolean selected) { }
  }

  private static final class TestCustomAction extends BattleAction implements BridgedBattleAction {
    @Override public void draw(final Battle battle, final int index, final boolean selected) { }
  }
}