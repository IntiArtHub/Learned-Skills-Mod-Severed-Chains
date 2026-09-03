package skill.system.menu;

import skill.system.api.SkillDefinition;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Pure presentation model for the Skills screen. */
public final class SkillScreenModel {
  public enum State { LEARNING_NOW, NOT_LEARNING, MASTERED }

  public record Entry(SkillDefinition skill, int rank, int mastery, int target, State state, List<String> effectTranslationKeys) {
    public String progress() { return this.mastery + " / " + this.target; }
  }

  private SkillScreenModel() { }

  public static Optional<Entry> entry(final SkillDefinition skill, final int rawMastery, final boolean manualEquipped) {
    final int mastery = skill.clamp(rawMastery);
    if(mastery < 1) return Optional.empty();
    final int rank = skill.rankFor(mastery);
    final boolean mastered = mastery >= skill.maximumMastery();
    final State state = mastered ? State.MASTERED : manualEquipped ? State.LEARNING_NOW : State.NOT_LEARNING;
    return Optional.of(new Entry(skill, rank, mastery, skill.nextThreshold(mastery), state, skill.effectTranslationKeys(rank)));
  }

  public static List<Entry> entries(
    final List<SkillDefinition> skills,
    final Map<String, Integer> masteryBySkill,
    final Set<String> equippedManualIds
  ) {
    return skills.stream()
      .sorted(Comparator.comparing(SkillDefinition::id))
      .map(skill -> entry(skill, masteryBySkill.getOrDefault(skill.id(), 0), equippedManualIds.contains(skill.manualId())))
      .flatMap(Optional::stream)
      .toList();
  }
}
