package skill.system;

import org.junit.jupiter.api.Test;
import skill.system.api.SkillDefinition;
import skill.system.api.SkillMasteryStore;
import skill.system.persistence.SkillMasteryCodec;

import static org.junit.jupiter.api.Assertions.*;

class SkillMasteryStoreTest {
  private static final SkillDefinition STEAL = new SkillDefinition("skill_system:steal", "skill_system:steal_manual", 20, 60, 99);

  @Test void ranksAndMasteryAreCumulativeAndCapped() {
    final SkillMasteryStore store = new SkillMasteryStore();
    final String dart = "lod:dart";
    assertEquals(1, store.getRank(dart, STEAL));
    store.addMastery(dart, STEAL, 19);
    assertEquals(1, store.getRank(dart, STEAL));
    assertEquals(2, store.addMastery(dart, STEAL, 1).newRank());
    store.addMastery(dart, STEAL, 39);
    assertEquals(3, store.addMastery(dart, STEAL, 1).newRank());
    store.addMastery(dart, STEAL, 38);
    assertTrue(store.addMastery(dart, STEAL, 1).newlyMastered());
    assertEquals(4, store.getRank(dart, STEAL));
    store.addMastery(dart, STEAL, 500);
    assertEquals(99, store.getMastery(dart, STEAL));
  }

  @Test void charactersHaveIndependentProgress() {
    final SkillMasteryStore store = new SkillMasteryStore();
    store.addMastery("lod:dart", STEAL, 98);
    store.addMastery("lod:rose", STEAL, 75);
    assertEquals(98, store.getMastery("lod:dart", STEAL));
    assertEquals(75, store.getMastery("lod:rose", STEAL));
  }

  @Test void versionedSavePayloadRoundTripsAndEmptyOldSaveDefaultsCleanly() {
    final SkillMasteryStore original = new SkillMasteryStore();
    original.addMastery("lod:dart", STEAL, 78);
    final byte[] encoded = SkillMasteryCodec.encode(original.snapshot());
    final SkillMasteryStore restored = new SkillMasteryStore();
    restored.replaceWith(SkillMasteryCodec.decode(encoded));
    assertEquals(78, restored.getMastery("lod:dart", STEAL));
    assertTrue(SkillMasteryCodec.decode(new byte[0]).isEmpty());
  }

  @Test void persistenceCoversAllRanksMultipleCharactersAndFutureSkills() {
    final SkillDefinition quickchange = new SkillDefinition("skill_system:quickchange", "skill_system:quickchange_manual", 10, 30);
    final SkillMasteryStore original = new SkillMasteryStore();
    original.setMastery("lod:dart", STEAL, 19);
    original.setMastery("lod:rose", STEAL, 20);
    original.setMastery("lod:haschel", STEAL, 60);
    original.setMastery("lod:meru", STEAL, 99);
    original.setMastery("lod:dart", quickchange, 9);

    final SkillMasteryStore restored = new SkillMasteryStore();
    restored.replaceWith(SkillMasteryCodec.decode(SkillMasteryCodec.encode(original.snapshot())));
    assertEquals(19, restored.getMastery("lod:dart", STEAL));
    assertEquals(20, restored.getMastery("lod:rose", STEAL));
    assertEquals(60, restored.getMastery("lod:haschel", STEAL));
    assertEquals(99, restored.getMastery("lod:meru", STEAL));
    assertEquals(9, restored.getMastery("lod:dart", quickchange));
    assertEquals(0, restored.getMastery("lod:unknown", STEAL));
  }

  @Test void malformedOrUnknownPayloadIsRejectedWithoutMutatingAStore() {
    final SkillMasteryStore store = new SkillMasteryStore();
    store.setMastery("lod:dart", STEAL, 20);
    assertThrows(IllegalArgumentException.class, () -> SkillMasteryCodec.decode(new byte[] {1, 2, 3, 4}));
    assertEquals(20, store.getMastery("lod:dart", STEAL));
  }
}
