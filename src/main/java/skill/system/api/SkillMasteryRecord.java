package skill.system.api;

/** Persisted state; rank is deliberately derived from the definition. */
public record SkillMasteryRecord(int mastery) {
  public SkillMasteryRecord {
    if(mastery < 0) {
      throw new IllegalArgumentException("mastery cannot be negative");
    }
  }
}
