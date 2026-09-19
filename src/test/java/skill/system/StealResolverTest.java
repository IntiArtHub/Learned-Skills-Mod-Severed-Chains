package skill.system;

import org.junit.jupiter.api.Test;
import skill.system.steal.HeldResource;
import skill.system.steal.HeldResourceState;
import skill.system.steal.EnemyHeldStateStore;
import skill.system.steal.StealResolver;

import java.util.random.RandomGenerator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StealResolverTest {
  private final StealResolver resolver = new StealResolver();

  @Test void probabilityUsesMultiplicationAndClamp() {
    assertEquals(2, resolver.finalChance(2, 1));
    assertEquals(4, resolver.finalChance(2, 2));
    assertEquals(6, resolver.finalChance(2, 3));
    assertEquals(8, resolver.finalChance(2, 4));
    assertEquals(100, resolver.finalChance(50, 2));
  }

  @Test void fullInventoryPreservesResourceForRetry() {
    final HeldResourceState state = new HeldResourceState(new HeldResource(HeldResource.ResourceType.ITEM, "item", 100));
    assertEquals(StealResolver.Outcome.INVENTORY_FULL, resolver.resolve(state, 1, RandomGenerator.getDefault(), ignored -> false));
    assertTrue(state.hasAvailableResource());
    assertFalse(state.stolen());
  }

  @Test void successfulResourceCanOnlyBeTakenOnce() {
    final HeldResourceState state = new HeldResourceState(new HeldResource(HeldResource.ResourceType.GOLD, 25, 100));
    assertEquals(StealResolver.Outcome.SUCCESS, resolver.resolve(state, 1, RandomGenerator.getDefault(), ignored -> true));
    assertEquals(StealResolver.Outcome.NOTHING, resolver.resolve(state, 1, RandomGenerator.getDefault(), ignored -> fail("award called twice")));
  }

  @Test void failedRollDoesNotChangeHeldResource() {
    final Object item = new Object();
    final HeldResourceState state = new HeldResourceState(new HeldResource(HeldResource.ResourceType.ITEM, item, 0));
    assertEquals(StealResolver.Outcome.FAILED, resolver.resolve(state, 1, RandomGenerator.getDefault(), ignored -> true));
    assertSame(item, state.resource().value());
    assertTrue(state.hasAvailableResource());
  }

  @Test void enemyInstancesAreIndependentAndBattleClearResetsEverything() {
    final EnemyHeldStateStore store = new EnemyHeldStateStore();
    final Object first = new Object();
    final Object second = new Object();
    final RandomGenerator alwaysHolding = new RandomGenerator() {
      @Override public long nextLong() { return -1L; }
    };
    store.create(first, List.of(new HeldResource(HeldResource.ResourceType.ITEM, "a", 10)), 0, false, alwaysHolding);
    store.create(second, List.of(new HeldResource(HeldResource.ResourceType.ITEM, "b", 10)), 0, false, alwaysHolding);
    assertNotSame(store.get(first), store.get(second));
    assertEquals(2, store.size());
    store.clear();
    assertEquals(0, store.size());
  }

  @Test void heldResourceIsIdentityScoped() {
    final EnemyHeldStateStore store = new EnemyHeldStateStore();
    final Object enemy = new Object();
    final HeldResource gold = new HeldResource(HeldResource.ResourceType.GOLD, 20, 100);
    final RandomGenerator alwaysHolding = new RandomGenerator() {
      @Override public long nextLong() { return -1L; }
    };
    assertSame(gold, store.create(enemy, List.of(gold), 0, false, alwaysHolding).resource());
    assertSame(gold, store.get(enemy).resource());
    assertThrows(IllegalStateException.class, () -> store.create(enemy, List.of(gold), 0, false, alwaysHolding));
  }
}
