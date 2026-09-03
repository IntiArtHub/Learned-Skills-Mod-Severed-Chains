package skill.system.battle;

import legend.game.combat.Battle;
import legend.game.combat.ui.BattleAction;
import legend.game.scripting.FlowControl;
import legend.game.scripting.RunningScript;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import static legend.game.Scus94491BpeSegment_8004.engineStateFunctions_8004e29c;

/** Keeps opted-in Java battle actions inside combat-menu function 160 until their tick lifecycle completes. */
public final class CustomBattleActionBridge implements Function<RunningScript, FlowControl> {
  static final int COMBAT_MENU_FUNCTION = 160;

  private final Supplier<BattleAction> currentAction;
  private final Function<RunningScript, FlowControl> delegate;

  CustomBattleActionBridge(final Supplier<BattleAction> currentAction, final Function<RunningScript, FlowControl> delegate) {
    this.currentAction = Objects.requireNonNull(currentAction);
    this.delegate = Objects.requireNonNull(delegate);
  }

  public static void install(final Battle battle) {
    final Function<RunningScript, FlowControl> current = engineStateFunctions_8004e29c[COMBAT_MENU_FUNCTION];
    if(current == null) {
      throw new IllegalStateException("Combat-menu script function 160 is unavailable");
    }
    if(current instanceof CustomBattleActionBridge) return;

    engineStateFunctions_8004e29c[COMBAT_MENU_FUNCTION] = new CustomBattleActionBridge(
      () -> battle.hud.battleMenu_800c6c34.currentAction,
      current
    );
  }

  @Override
  public FlowControl apply(final RunningScript script) {
    final boolean actionWasAlreadyActive = this.currentAction.get() != null;
    final FlowControl flow = this.delegate.apply(script);
    final BattleAction action = this.currentAction.get();

    if(!actionWasAlreadyActive && flow == FlowControl.CONTINUE && action instanceof BridgedBattleAction) {
      return FlowControl.PAUSE_AND_REWIND;
    }

    return flow;
  }
}