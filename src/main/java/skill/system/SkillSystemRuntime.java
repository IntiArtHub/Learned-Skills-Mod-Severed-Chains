package skill.system;

import legend.game.characters.CharacterData2c;
import legend.game.inventory.Equipment;
import legend.game.types.EquipmentSlot;
import skill.system.api.MasteryChange;
import skill.system.api.SkillDefinition;
import skill.system.api.SkillMasteryStore;
import skill.system.persistence.SkillMasteryCodec;

import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static legend.core.GameEngine.CONFIG;
import static legend.game.Scus94491BpeSegment_800b.gameState_800babc8;

/** Engine-facing facade. Progress is always resolved from stable template registry IDs. */
public final class SkillSystemRuntime {
  private static final Logger LOGGER = LogManager.getFormatterLogger(SkillSystemRuntime.class);
  public static final SkillDefinition STEAL = new SkillDefinition(
    "skill_system:steal",
    "skill_system:steal_manual",
    "skill_system.skill.steal.name",
    "skill_system.skill.steal.description",
    List.of(
      List.of("skill_system.skill.steal.effect.rank_1"),
      List.of("skill_system.skill.steal.effect.rank_2"),
      List.of("skill_system.skill.steal.effect.rank_3", "skill_system.skill.steal.effect.boss"),
      List.of("skill_system.skill.steal.effect.rank_4", "skill_system.skill.steal.effect.boss")
    ),
    20, 60, 99
  );
  public static final SkillDefinition BUFF_DANCE = new SkillDefinition(
    "skill_system:buff_dance", "skill_system:buff_dance_manual",
    "skill_system.skill.buff_dance.name", "skill_system.skill.buff_dance.description",
    List.of(
      List.of("skill_system.skill.buff_dance.effect.rank_1"),
      List.of("skill_system.skill.buff_dance.effect.rank_2"),
      List.of("skill_system.skill.buff_dance.effect.rank_3"),
      List.of("skill_system.skill.buff_dance.effect.rank_4")
    ), 20, 60, 99
  );
  public static final SkillDefinition QUICKCHANGE = new SkillDefinition(
    "skill_system:quickchange", "skill_system:quickchange_manual",
    "skill_system.skill.quickchange.name", "skill_system.skill.quickchange.description",
    List.of(
      List.of("skill_system.skill.quickchange.effect.rank_1"),
      List.of("skill_system.skill.quickchange.effect.rank_2"),
      List.of("skill_system.skill.quickchange.effect.rank_3"),
      List.of("skill_system.skill.quickchange.effect.rank_4")
    ), 20, 60, 99
  );
  private static final List<SkillDefinition> SKILLS = List.of(STEAL, BUFF_DANCE, QUICKCHANGE);
  public static final SkillMasteryStore MASTERY = new SkillMasteryStore();

  private SkillSystemRuntime() { }

  public static List<SkillDefinition> skills() { return SKILLS; }

  public static String characterId(final CharacterData2c character) {
    return character.template.getRegistryId().toString();
  }

  public static boolean hasPermanentSkill(final CharacterData2c character, final SkillDefinition skill) {
    return MASTERY.isMastered(characterId(character), skill);
  }

  public static boolean isSkillManualEquipped(final CharacterData2c character, final SkillDefinition skill) {
    final Equipment accessory = character.getEquipment(EquipmentSlot.ACCESSORY);
    return accessory != null && accessory.getRegistryId().toString().equals(skill.manualId());
  }

  public static boolean canUseSkill(final CharacterData2c character, final SkillDefinition skill) {
    return hasPermanentSkill(character, skill) || isSkillManualEquipped(character, skill);
  }

  public static int getSkillMastery(final CharacterData2c character, final SkillDefinition skill) {
    return MASTERY.getMastery(characterId(character), skill);
  }

  public static int getSkillRank(final CharacterData2c character, final SkillDefinition skill) {
    return MASTERY.getRank(characterId(character), skill);
  }

  public static MasteryChange addSkillMastery(final CharacterData2c character, final SkillDefinition skill, final int amount) {
    return addSkillMastery(character, skill, amount, false);
  }

  /** Manual provenance is needed when Quickchange moved its granting manual into inventory. */
  public static MasteryChange addSkillMastery(final CharacterData2c character, final SkillDefinition skill,
                                              final int amount, final boolean usedThroughManual) {
    final boolean manualEquipped = isSkillManualEquipped(character, skill);
    final MasteryChange change = MASTERY.addMastery(characterId(character), skill, amount);
    persist();
    if(change.newlyMastered()) {
      if(manualEquipped) {
        character.equip(EquipmentSlot.ACCESSORY, null);
      } else if(skill == QUICKCHANGE && usedThroughManual) {
        final int index = findMatchingManualInInventory(skill.manualId());
        if(index >= 0) gameState_800babc8.equipment_1e8.remove(index);
        else LOGGER.warn("[Learned Skills] Quickchange mastery reached but no matching manual remained in inventory for %s", characterId(character));
      }
    }
    return change;
  }

  private static int findMatchingManualInInventory(final String manualId) {
    for(int i = 0; i < gameState_800babc8.equipment_1e8.size(); i++) {
      if(gameState_800babc8.equipment_1e8.get(i).getRegistryId().toString().equals(manualId)) return i;
    }
    return -1;
  }

  public static void load() {
    final byte[] bytes = CONFIG.getConfig(SkillSystemMod.MASTERY_CONFIG.get());
    MASTERY.replaceWith(SkillMasteryCodec.decode(bytes));
  }

  public static void persist() {
    CONFIG.setConfig(SkillSystemMod.MASTERY_CONFIG.get(), SkillMasteryCodec.encode(MASTERY.snapshot()));
  }
}
