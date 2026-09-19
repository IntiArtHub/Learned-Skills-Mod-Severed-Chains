package skill.system.buffdance;

import legend.game.characters.StatMod;
import legend.game.characters.UnaryStat;
import legend.game.characters.UnaryStatMod;

/** Battle-local percentage modifier. Native turn ticking and integer rounding apply. */
public final class DanceStatMod extends UnaryStatMod {
  public DanceStatMod(final int percentage, final int turns) {
    super(percentage, true, turns, false);
  }

  public int percentage() { return this.amount; }

  @Override
  public StatMod<UnaryStat> copy() { return new DanceStatMod(this.amount, this.turns); }
}
