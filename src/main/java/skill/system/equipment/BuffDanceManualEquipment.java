package skill.system.equipment;

import legend.game.characters.CharacterData2c;
import legend.game.characters.ElementSet;
import legend.game.inventory.CanEquip;
import legend.game.inventory.Equipment;
import legend.game.inventory.ItemIcon;
import legend.game.types.Renderable58;
import legend.game.types.EquipmentSlot;
import legend.lodmod.LodMod;
import skill.system.SkillSystemRuntime;

public final class BuffDanceManualEquipment extends Equipment {
  public BuffDanceManualEquipment() {
    super(10, 0, EquipmentSlot.ACCESSORY, LodMod.NO_ELEMENT.get(), new ElementSet(), new ElementSet(),
      0, 0, 0, 0, 0, 0, 0, 0, false, false, false, false,
      0, 0, 0, 0, 0, ItemIcon.NONE, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
  }

  @Override public Renderable58 renderIcon(final int x, final int y, final int flags) {
    return ManualAtlasIcon.BUFF_DANCE.renderInventory(this, x, y, flags);
  }

  @Override public Renderable58 renderIconManual(final int x, final int y, final int flags) {
    return ManualAtlasIcon.BUFF_DANCE.renderInventoryManual(this, x, y, flags);
  }

  @Override public CanEquip canEquip(final CharacterData2c character, final EquipmentSlot slot) {
    return SkillSystemRuntime.hasPermanentSkill(character, SkillSystemRuntime.BUFF_DANCE) ? CanEquip.DENY : CanEquip.FORCE;
  }
}
