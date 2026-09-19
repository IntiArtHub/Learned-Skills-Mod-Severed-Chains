package skill.system.quickchange;

import legend.game.combat.bent.PlayerBattleEntity;
import legend.game.inventory.Equipment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import legend.game.types.EquipmentSlot;

import static legend.game.Scus94491BpeSegment_800b.gameState_800babc8;
import static legend.game.SItem.giveEquipment;
import static legend.game.SItem.takeEquipment;

/** One validated inventory/equipment exchange, followed by the proven live sync. */
final class QuickchangeEquipmentTransaction {
  private static final Logger LOGGER = LogManager.getFormatterLogger(QuickchangeEquipmentTransaction.class);

  private QuickchangeEquipmentTransaction() { }

  static boolean commit(final PlayerBattleEntity player, final Equipment incoming) {
    return commitAll(player, Map.of(incoming.slot, incoming));
  }

  static boolean commitAll(final PlayerBattleEntity player, final Map<EquipmentSlot, Equipment> changes) {
    if(changes.isEmpty()) return false;
    final List<Equipment> inventory = gameState_800babc8.equipment_1e8;
    final List<Equipment> original = new ArrayList<>(inventory);
    final List<Equipment> expected = new ArrayList<>(original);
    for(final EquipmentSlot slot : EquipmentSlot.values()) {
      final Equipment incoming = changes.get(slot);
      if(incoming == null) continue;
      if(incoming.slot != slot || !QuickchangeEquipmentScope.canSwap(player, incoming)) return false;
      if(!expected.remove(incoming)) return false;
      expected.add(player.character.getEquipment(slot));
    }

    // Complete every event-backed inventory exchange before touching live equipment.
    final List<Equipment> step = new ArrayList<>(original);
    for(final EquipmentSlot slot : EquipmentSlot.values()) {
      final Equipment incoming = changes.get(slot);
      if(incoming == null) continue;
      final Equipment outgoing = player.character.getEquipment(slot);
      final int index = step.indexOf(incoming);
      if(index < 0 || !takeEquipment(index)) {
        restore(inventory, original);
        return false;
      }
      step.remove(index);
      if(!inventory.equals(step) || !giveEquipment(outgoing)) {
        restore(inventory, original);
        LOGGER.warn("[Quickchange] Inventory event changed an exchange; loadout cancelled");
        return false;
      }
      step.add(outgoing);
      if(!inventory.equals(step)) {
        restore(inventory, original);
        LOGGER.warn("[Quickchange] Inventory event changed an exchange; loadout cancelled");
        return false;
      }
    }
    if(!inventory.equals(expected)) {
      restore(inventory, original);
      return false;
    }
    for(final EquipmentSlot slot : EquipmentSlot.values()) {
      final Equipment incoming = changes.get(slot);
      if(incoming == null) continue;
      final Equipment outgoing = player.character.getEquipment(slot);
      player.character.equip(slot, incoming);
      QuickchangeEquipmentSync.apply(player, outgoing, incoming);
      LOGGER.info("[Quickchange] %s: %s -> %s", player.getName(), outgoing.getRegistryId(), incoming.getRegistryId());
    }
    return true;
  }

  private static void restore(final List<Equipment> inventory, final List<Equipment> original) {
    inventory.clear();
    inventory.addAll(original);
  }
}
