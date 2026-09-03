package skill.system.steal;

import java.util.Objects;
import java.util.random.RandomGenerator;

/** Pure Steal resolution. Awarding is attempted only after the probability roll succeeds. */
public final class StealResolver {
  public enum Outcome { SUCCESS, FAILED, NOTHING, INVENTORY_FULL }

  public record Resolution(Outcome outcome, int baseChance, int finalChance, int roll) { }

  @FunctionalInterface
  public interface Awarder { boolean award(HeldResource resource); }

  public int finalChance(final int baseChance, final int rank) {
    if(baseChance < 0 || baseChance > 100) throw new IllegalArgumentException("baseChance must be 0..100");
    if(rank < 1) throw new IllegalArgumentException("rank must be positive");
    return Math.min(100, baseChance * rank);
  }

  public Outcome resolve(final HeldResourceState state, final int rank, final RandomGenerator rng, final Awarder awarder) {
    return this.resolveDetailed(state, rank, rng, awarder).outcome();
  }

  public Resolution resolveDetailed(final HeldResourceState state, final int rank, final RandomGenerator rng, final Awarder awarder) {
    return this.resolveDetailed(state, rank, rng, awarder, false);
  }

  public Resolution resolveDetailed(final HeldResourceState state, final int rank, final RandomGenerator rng,
                                    final Awarder awarder, final boolean forceSuccess) {
    Objects.requireNonNull(state, "state");
    if(!state.hasAvailableResource()) return new Resolution(Outcome.NOTHING, 0, 0, -1);
    final HeldResource resource = state.resource();
    final int chance = this.finalChance(resource.baseChance(), rank);
    final int roll = forceSuccess ? -2 : rng.nextInt(100);
    if(forceSuccess) {
      if(!awarder.award(resource)) return new Resolution(Outcome.INVENTORY_FULL, resource.baseChance(), chance, roll);
      state.markStolen();
      return new Resolution(Outcome.SUCCESS, resource.baseChance(), chance, roll);
    }
    if(roll >= chance) return new Resolution(Outcome.FAILED, resource.baseChance(), chance, roll);
    if(!awarder.award(resource)) return new Resolution(Outcome.INVENTORY_FULL, resource.baseChance(), chance, roll);
    state.markStolen();
    return new Resolution(Outcome.SUCCESS, resource.baseChance(), chance, roll);
  }
}
