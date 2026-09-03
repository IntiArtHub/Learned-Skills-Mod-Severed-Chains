package skill.system.steal;

import java.util.Objects;

public record HeldResource(ResourceType type, Object value, int baseChance) {
  public HeldResource {
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(value, "value");
    if(baseChance < 0 || baseChance > 100) throw new IllegalArgumentException("base chance must be 0..100");
  }

  public enum ResourceType { ITEM, EQUIPMENT, GOLD }
}
