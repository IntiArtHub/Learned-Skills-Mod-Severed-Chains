package skill.system.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Character registry ID -> skill registry ID -> mastery record. */
public final class SkillMasteryStore {
  private final Map<String, Map<String, SkillMasteryRecord>> records = new LinkedHashMap<>();

  public int getMastery(final String characterId, final SkillDefinition skill) {
    return this.records.getOrDefault(characterId, Map.of()).getOrDefault(skill.id(), new SkillMasteryRecord(0)).mastery();
  }

  public int getRank(final String characterId, final SkillDefinition skill) {
    return skill.rankFor(this.getMastery(characterId, skill));
  }

  public boolean isMastered(final String characterId, final SkillDefinition skill) {
    return this.getMastery(characterId, skill) >= skill.maximumMastery();
  }

  public MasteryChange addMastery(final String characterId, final SkillDefinition skill, final int amount) {
    if(amount < 0) throw new IllegalArgumentException("Mastery cannot be removed through addMastery");
    final int oldValue = this.getMastery(characterId, skill);
    final int newValue = skill.clamp(oldValue + amount);
    if(newValue > 0) {
      this.records.computeIfAbsent(characterId, ignored -> new LinkedHashMap<>()).put(skill.id(), new SkillMasteryRecord(newValue));
    }
    return new MasteryChange(oldValue, newValue, skill.rankFor(oldValue), skill.rankFor(newValue), oldValue < skill.maximumMastery() && newValue == skill.maximumMastery());
  }

  /** Explicit setter reserved for controlled diagnostics and data migration. */
  public MasteryChange setMastery(final String characterId, final SkillDefinition skill, final int mastery) {
    final int oldValue = this.getMastery(characterId, skill);
    final int newValue = skill.clamp(mastery);
    if(newValue == 0) {
      final Map<String, SkillMasteryRecord> skills = this.records.get(characterId);
      if(skills != null) {
        skills.remove(skill.id());
        if(skills.isEmpty()) this.records.remove(characterId);
      }
    } else {
      this.records.computeIfAbsent(characterId, ignored -> new LinkedHashMap<>()).put(skill.id(), new SkillMasteryRecord(newValue));
    }
    return new MasteryChange(oldValue, newValue, skill.rankFor(oldValue), skill.rankFor(newValue),
      oldValue < skill.maximumMastery() && newValue == skill.maximumMastery());
  }

  public Map<String, Map<String, SkillMasteryRecord>> snapshot() {
    final Map<String, Map<String, SkillMasteryRecord>> result = new LinkedHashMap<>();
    this.records.forEach((character, skills) -> result.put(character, Collections.unmodifiableMap(new LinkedHashMap<>(skills))));
    return Collections.unmodifiableMap(result);
  }

  public void replaceWith(final Map<String, Map<String, SkillMasteryRecord>> loaded) {
    this.records.clear();
    loaded.forEach((character, skills) -> this.records.put(character, new LinkedHashMap<>(skills)));
  }

  public void clear() { this.records.clear(); }
}
