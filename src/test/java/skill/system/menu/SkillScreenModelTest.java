package skill.system.menu;

import org.junit.jupiter.api.Test;
import skill.system.SkillSystemMod;
import skill.system.api.SkillDefinition;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SkillScreenModelTest {
  private static final SkillDefinition STEAL = new SkillDefinition(
    "skill_system:steal", "skill_system:steal_manual", "steal.name", "steal.description",
    List.of(List.of("steal.1"), List.of("steal.2"), List.of("steal.3", "steal.boss"), List.of("steal.4", "steal.boss")),
    20, 60, 99
  );
  private static final SkillDefinition QUICKCHANGE = new SkillDefinition(
    "skill_system:quickchange", "skill_system:quickchange_manual", "quick.name", "quick.description",
    List.of(List.of("quick.1"), List.of("quick.2")), 50
  );

  @Test void masteryZeroIsHiddenAndEmptyCharacterProducesNoRows() {
    assertTrue(SkillScreenModel.entry(STEAL, 0, true).isEmpty());
    assertTrue(SkillScreenModel.entries(List.of(STEAL), Map.of(), Set.of()).isEmpty());
  }

  @Test void masteryOneIsVisibleAndEquippedManualMeansLearningNow() {
    final var entry = SkillScreenModel.entry(STEAL, 1, true).orElseThrow();
    assertEquals(1, entry.rank());
    assertEquals("1 / 20", entry.progress());
    assertEquals(SkillScreenModel.State.LEARNING_NOW, entry.state());
    assertEquals(List.of("steal.1"), entry.effectTranslationKeys());
  }

  @Test void unequippedManualMeansNotLearning() {
    assertEquals(SkillScreenModel.State.NOT_LEARNING, SkillScreenModel.entry(STEAL, 19, false).orElseThrow().state());
  }

  @Test void levelsUseCumulativeDenominatorsAndRankEffects() {
    final var level1 = SkillScreenModel.entry(STEAL, 19, false).orElseThrow();
    final var level2 = SkillScreenModel.entry(STEAL, 20, false).orElseThrow();
    final var level3 = SkillScreenModel.entry(STEAL, 60, false).orElseThrow();
    final var level4 = SkillScreenModel.entry(STEAL, 99, false).orElseThrow();
    assertEquals(List.of(1, 2, 3, 4), List.of(level1.rank(), level2.rank(), level3.rank(), level4.rank()));
    assertEquals(List.of("19 / 20", "20 / 60", "60 / 99", "99 / 99"), List.of(level1.progress(), level2.progress(), level3.progress(), level4.progress()));
    assertEquals(List.of("steal.1"), level1.effectTranslationKeys());
    assertEquals(List.of("steal.2"), level2.effectTranslationKeys());
    assertEquals(List.of("steal.3", "steal.boss"), level3.effectTranslationKeys());
    assertEquals(List.of("steal.4", "steal.boss"), level4.effectTranslationKeys());
    assertEquals(SkillScreenModel.State.MASTERED, level4.state());
  }

  @Test void multipleCharactersRemainIsolated() {
    final Map<String, Map<String, Integer>> characters = Map.of(
      "lod:dart", Map.of(STEAL.id(), 37),
      "lod:lavitz", Map.of(STEAL.id(), 78)
    );
    assertEquals("37 / 60", SkillScreenModel.entries(List.of(STEAL), characters.get("lod:dart"), Set.of()).getFirst().progress());
    assertEquals("78 / 99", SkillScreenModel.entries(List.of(STEAL), characters.get("lod:lavitz"), Set.of()).getFirst().progress());
  }

  @Test void multipleSkillsSortIndependentlyByStableId() {
    final var entries = SkillScreenModel.entries(List.of(STEAL, QUICKCHANGE), Map.of(STEAL.id(), 25, QUICKCHANGE.id(), 5), Set.of(QUICKCHANGE.manualId()));
    assertEquals(List.of(QUICKCHANGE.id(), STEAL.id()), entries.stream().map(entry -> entry.skill().id()).toList());
    assertEquals(SkillScreenModel.State.LEARNING_NOW, entries.getFirst().state());
    assertEquals(SkillScreenModel.State.NOT_LEARNING, entries.getLast().state());
  }

  @Test void menuAddonContractUsesStableIdAndLocalizedLabelKey() {
    assertEquals("skill_system:skills", SkillSystemMod.SKILLS_MENU_ID.toString());
    assertEquals("skill_system.ui.skills", SkillSystemMod.SKILLS_MENU_NAME_KEY);
  }
}
