package skill.system.persistence;

import legend.game.saves.ConfigCategory;
import legend.game.saves.ConfigEntry;
import legend.game.saves.ConfigStorageLocation;

/** Invisible save-scoped data serialized by the native save config storage. */
public final class SkillMasteryConfig extends ConfigEntry<byte[]> {
  public SkillMasteryConfig() {
    super(new byte[0], ConfigStorageLocation.SAVE, ConfigCategory.OTHER, bytes -> bytes, bytes -> bytes);
  }

  @Override
  public boolean availableInBattle() { return false; }
}
