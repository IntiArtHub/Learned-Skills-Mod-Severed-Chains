package skill.system.quickchange;

import legend.game.combat.bent.PlayerBattleEntity;
import legend.game.inventory.Equipment;
import legend.lodmod.LodMod;
import legend.lodmod.equipment.UltimateWargodEquipment;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static legend.game.Scus94491BpeSegment_800b.gameState_800babc8;

/** Stock equipment policy, limited to contributions handled by QuickchangeEquipmentSync. */
final class QuickchangeEquipmentScope {
  private static final String QUICKCHANGE_MANUAL = "skill_system:quickchange_manual";

  private QuickchangeEquipmentScope() { }

  static List<Equipment> candidates(final PlayerBattleEntity player) {
    final Set<Equipment> unique = new LinkedHashSet<>();
    for(final Equipment equipment : gameState_800babc8.equipment_1e8) {
      if(canSwap(player, equipment)) unique.add(equipment);
    }
    return new ArrayList<>(unique);
  }

  static boolean canSwap(final PlayerBattleEntity player, final Equipment incoming) {
    final Equipment outgoing = player.character.getEquipment(incoming.slot);
    return canStage(player, outgoing, incoming) &&
      gameState_800babc8.equipment_1e8.contains(incoming);
  }

  static boolean canStage(final PlayerBattleEntity player, final Equipment outgoing, final Equipment incoming) {
    if(!supported(incoming)) return false;
    // The granting Manual has no numeric/effect contribution and may be removed.
    // Incoming Manuals remain blocked until dynamic skill introduction is handled.
    if(outgoing == null || outgoing == incoming) return false;
    final String outgoingId = outgoing.getRegistryId().toString();
    if(!supported(outgoing) && !QUICKCHANGE_MANUAL.equals(outgoingId)) return false;
    if(player.equipment_11e.get(incoming.slot) != outgoing) return false;
    return player.character.canEquip(incoming.slot, incoming);
  }

  private static boolean supported(final Equipment item) {
    if(item == null || !item.getRegistryId().toString().startsWith("lod:")) return false;
    if(item instanceof UltimateWargodEquipment) return item.getRegistryId().toString().equals("lod:ultimate_wargod");
    if(item.getClass() != Equipment.class) return false; // No untracked applyEffect/attack callbacks.
    if(item.flags_00 != 0 || item.attackElement_04.size() != 1 ||
      !item.attackElement_04.contains(LodMod.NO_ELEMENT.get()) ||
      !item.elementalResistance_06.isEmpty() || !item.elementalImmunity_07.isEmpty()) return false;
    return item.onHitStatusChance_18 == 0 && item.onHitStatus_1b == 0 &&
      !item.physicalImmunity && !item.magicalImmunity &&
      !item.physicalResistance && !item.magicalResistance &&
      item.spMultiplier == 0 && item.spPerPhysicalHit == 0 && item.mpPerPhysicalHit == 0 &&
      item.spPerMagicalHit == 0 && item.mpPerMagicalHit == 0 &&
      item.hpMultiplier == 0 && item.mpMultiplier == 0 &&
      item.escapeBonus == 0 && item.revive == 0 &&
      item.mpRegen == 0 && item.spRegen == 0;
  }
}
