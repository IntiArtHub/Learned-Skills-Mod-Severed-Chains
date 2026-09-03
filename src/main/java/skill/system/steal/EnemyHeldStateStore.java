package skill.system.steal;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

/** Battle-lifetime state keyed by enemy object identity, never species ID/equality. */
public final class EnemyHeldStateStore {
  private final Map<Object, HeldResourceState> states = new IdentityHashMap<>();

  public HeldResourceState create(final Object enemyInstance, final List<HeldResource> candidates,
                                  final int gold, final boolean boss, final RandomGenerator rng) {
    if(this.states.containsKey(enemyInstance)) throw new IllegalStateException("Enemy already initialized");
    final HeldResource resource;
    if(!rng.nextBoolean()) resource = null;
    else if(!candidates.isEmpty()) resource = candidates.get(rng.nextInt(candidates.size()));
    else resource = new HeldResource(HeldResource.ResourceType.GOLD, Math.max(0, gold), boss ? 7 : 50);
    final HeldResourceState state = new HeldResourceState(resource);
    this.states.put(enemyInstance, state);
    return state;
  }

  public HeldResourceState get(final Object enemyInstance) { return this.states.get(enemyInstance); }
  public HeldResourceState createForced(final Object enemyInstance, final HeldResource resource) {
    if(this.states.containsKey(enemyInstance)) throw new IllegalStateException("Enemy already initialized");
    final HeldResourceState state = new HeldResourceState(resource);
    this.states.put(enemyInstance, state);
    return state;
  }
  public int size() { return this.states.size(); }
  public void clear() { this.states.clear(); }
}
