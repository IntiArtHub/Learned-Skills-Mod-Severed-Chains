package skill.system.battle;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/** One credit per character/skill per victorious battle, never per button press. */
public final class BattleSkillUseLedger {
  private record Use(String character, String skill) { }
  private final Set<Use> uses = new HashSet<>();

  public void record(final String character, final String skill) { this.uses.add(new Use(character, skill)); }
  public void clear() { this.uses.clear(); }

  public void finish(final boolean victory, final Predicate<String> alive, final BiConsumer<String, String> award) {
    final Set<Use> completed = Set.copyOf(this.uses);
    this.clear(); // Consume before callbacks, including re-entrant or repeated end notifications.
    if(victory) for(final Use use : completed) if(alive.test(use.character)) award.accept(use.character, use.skill);
  }
}
