package skill.system;

import legend.game.characters.CharacterData2c;
import legend.game.inventory.Equipment;
import legend.game.types.EquipmentSlot;
import skill.system.api.MasteryChange;
import skill.system.api.SkillDefinition;
import skill.system.api.SkillMasteryStore;
import skill.system.persistence.SkillMasteryCodec;

import java.util.List;

import static legend.core.GameEngine.CONFIG;

/** Engine-facing facade. Progress is always resolved from stable template registry IDs. */
public final class SkillSystemRuntime {
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
  private static final List<SkillDefinition> SKILLS = List.of(STEAL);
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
    final MasteryChange change = MASTERY.addMastery(characterId(character), skill, amount);
    persist();
    if(change.newlyMastered() && isSkillManualEquipped(character, skill)) {
      character.equip(EquipmentSlot.ACCESSORY, null);
    }
    return change;
  }

  public static MasteryChange setSkillMasteryForTesting(final CharacterData2c character, final SkillDefinition skill, final int mastery) {
    if(!SkillSystemDiagnostics.enabled()) throw new IllegalStateException("Test mastery requires skill_system.debug=true");
    final MasteryChange change = MASTERY.setMastery(characterId(character), skill, mastery);
    persist();
    return change;
  }

  public static void load() {
    final byte[] bytes = CONFIG.getConfig(SkillSystemMod.MASTERY_CONFIG.get());
    MASTERY.replaceWith(SkillMasteryCodec.decode(bytes));
  }

  public static void persist() {
    CONFIG.setConfig(SkillSystemMod.MASTERY_CONFIG.get(), SkillMasteryCodec.encode(MASTERY.snapshot()));
  }
}
