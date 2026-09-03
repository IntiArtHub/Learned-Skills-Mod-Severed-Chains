package skill.system.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** Immutable rules and localized presentation metadata for one trainable skill. Thresholds are cumulative. */
public final class SkillDefinition {
  private final String id;
  private final String manualId;
  private final int[] thresholds;
  private final String nameTranslationKey;
  private final String descriptionTranslationKey;
  private final List<List<String>> rankEffectTranslationKeys;

  public SkillDefinition(final String id, final String manualId, final int... thresholds) {
    this(id, manualId, id + ".name", id + ".description", emptyEffects(thresholds.length + 1), thresholds);
  }

  public SkillDefinition(
    final String id,
    final String manualId,
    final String nameTranslationKey,
    final String descriptionTranslationKey,
    final List<List<String>> rankEffectTranslationKeys,
    final int... thresholds
  ) {
    this.id = requireId(id, "skill");
    this.manualId = requireId(manualId, "manual");
    this.nameTranslationKey = Objects.requireNonNull(nameTranslationKey, "nameTranslationKey");
    this.descriptionTranslationKey = Objects.requireNonNull(descriptionTranslationKey, "descriptionTranslationKey");
    if(thresholds.length == 0) throw new IllegalArgumentException("A skill needs at least one mastery threshold");
    this.thresholds = Arrays.copyOf(thresholds, thresholds.length);
    int previous = 0;
    for(final int threshold : this.thresholds) {
      if(threshold <= previous) throw new IllegalArgumentException("Mastery thresholds must be positive and strictly increasing");
      previous = threshold;
    }
    if(rankEffectTranslationKeys.size() != this.rankCount()) {
      throw new IllegalArgumentException("Rank effects must contain one entry per rank");
    }
    this.rankEffectTranslationKeys = rankEffectTranslationKeys.stream().map(List::copyOf).toList();
  }

  private static List<List<String>> emptyEffects(final int rankCount) {
    final List<List<String>> effects = new ArrayList<>(rankCount);
    for(int i = 0; i < rankCount; i++) effects.add(List.of());
    return effects;
  }

  private static String requireId(final String id, final String label) {
    final String value = Objects.requireNonNull(id, label + " ID");
    if(value.isBlank() || value.indexOf(':') < 1) throw new IllegalArgumentException(label + " ID must be a namespaced registry ID");
    return value;
  }

  public String id() { return this.id; }
  public String manualId() { return this.manualId; }
  public String nameTranslationKey() { return this.nameTranslationKey; }
  public String descriptionTranslationKey() { return this.descriptionTranslationKey; }
  public int maximumMastery() { return this.thresholds[this.thresholds.length - 1]; }
  public int rankCount() { return this.thresholds.length + 1; }
  public List<String> effectTranslationKeys(final int rank) {
    if(rank < 1 || rank > this.rankCount()) throw new IllegalArgumentException("Rank outside skill range: " + rank);
    return this.rankEffectTranslationKeys.get(rank - 1);
  }

  /** Ranks are one-based. A value exactly at a threshold enters the next rank. */
  public int rankFor(final int mastery) {
    final int clamped = this.clamp(mastery);
    int rank = 1;
    for(final int threshold : this.thresholds) {
      if(clamped < threshold) break;
      rank++;
    }
    return rank;
  }

  public int nextThreshold(final int mastery) {
    final int clamped = this.clamp(mastery);
    for(final int threshold : this.thresholds) if(clamped < threshold) return threshold;
    return this.maximumMastery();
  }

  public int clamp(final int mastery) { return Math.max(0, Math.min(mastery, this.maximumMastery())); }
}
