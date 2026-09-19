package skill.system.quickchange;

import legend.game.characters.UnaryStat;
import legend.game.combat.bent.PlayerBattleEntity;
import legend.game.inventory.Equipment;
import legend.lodmod.equipment.UltimateWargodEquipment;
import java.util.function.ToIntFunction;

import static legend.game.Scus94491BpeSegment_8006.battleState_8006e398;
import static legend.lodmod.LodMod.ATTACK_STAT;
import static legend.lodmod.LodMod.DEFENSE_STAT;
import static legend.lodmod.LodMod.MAGIC_DEFENSE_STAT;
import static legend.lodmod.LodMod.MAGIC_ATTACK_STAT;
import static legend.lodmod.LodMod.ATTACK_HIT_STAT;
import static legend.lodmod.LodMod.MAGIC_HIT_STAT;
import static legend.lodmod.LodMod.ATTACK_AVOID_STAT;
import static legend.lodmod.LodMod.MAGIC_AVOID_STAT;
import static legend.lodmod.LodMod.SPEED_STAT;

/** Targeted live-state synchronisation proven by the stock equipment probe. */
final class QuickchangeEquipmentSync {
  private QuickchangeEquipmentSync() { }

  static void apply(final PlayerBattleEntity player, final Equipment old, final Equipment next) {
    final int oldAttack = old == null ? 0 : old.attack_10;
    final int newAttack = next == null ? 0 : next.attack_10;
    final int oldDefence = old == null ? 0 : old.defence_12;
    final int newDefence = next == null ? 0 : next.defence_12;
    final int oldMagicDefence = old == null ? 0 : old.magicDefence_13;
    final int newMagicDefence = next == null ? 0 : next.magicDefence_13;

    delta(player.stats.getStat(ATTACK_STAT.get()), newAttack - oldAttack);
    delta(player.stats.getStat(MAGIC_ATTACK_STAT.get()), old, next, item -> item.magicAttack_11);
    delta(player.stats.getStat(DEFENSE_STAT.get()), newDefence - oldDefence);
    delta(player.stats.getStat(MAGIC_DEFENSE_STAT.get()), newMagicDefence - oldMagicDefence);
    delta(player.stats.getStat(ATTACK_HIT_STAT.get()), old, next, item -> item.attackHit_14);
    delta(player.stats.getStat(MAGIC_HIT_STAT.get()), old, next, item -> item.magicHit_15);
    delta(player.stats.getStat(ATTACK_AVOID_STAT.get()), old, next, item -> item.attackAvoid_16);
    delta(player.stats.getStat(MAGIC_AVOID_STAT.get()), old, next, item -> item.magicAvoid_17);
    player.equipmentAttack1_28 += newAttack - oldAttack;
    player.hpRegen_134 += (next == null ? 0 : next.hpRegen) - (old == null ? 0 : old.hpRegen);

    // The character's onEquip/onUnequip callbacks have already updated persistent SPEED.
    // Transfer only the equipment-owned modifier; temporary battle mods stay in place.
    final UnaryStat speed = player.stats.getStat(SPEED_STAT.get());
    if(old != null && old.speed_0f != 0) speed.removeMod(old.getRegistryId());
    if(next != null && next.speed_0f != 0) {
      speed.addMod(next.getRegistryId(), player.character.stats.getStat(SPEED_STAT.get()).getMod(next.getRegistryId()).copy());
    }

    if(next == null) player.equipment_11e.remove(old.slot);
    else player.equipment_11e.put(next.slot, next);

    // This legacy field is equipment-only; rebuild it so Yore's resistance is removed too.
    int statusResist = 0;
    for(final Equipment equipped : player.equipment_11e.values()) statusResist |= equipped.statusResist_08;
    player.equipmentStatusResist_24 = statusResist;

    // Ultimate Wargod's applyEffect sets the equipment-owned 0x6 bits during
    // battle init. Update those bits from the final loadout without touching
    // Destroyer Mace's 0x1 bit or AdditionExtra's unrelated state.
    if(old instanceof UltimateWargodEquipment || next instanceof UltimateWargodEquipment) {
      final var addition = battleState_8006e398.additionExtra_474[player.allBentSlot_274];
      addition.flag_00 &= ~0x6;
      if(player.equipment_11e.values().stream().anyMatch(UltimateWargodEquipment.class::isInstance)) {
        addition.flag_00 |= 0x6;
      }
    }
  }

  private static void delta(final UnaryStat stat, final int amount) {
    stat.setRaw(stat.getRaw() + amount);
  }

  private static void delta(final UnaryStat stat, final Equipment old, final Equipment next,
                            final ToIntFunction<Equipment> contribution) {
    delta(stat, (next == null ? 0 : contribution.applyAsInt(next)) -
      (old == null ? 0 : contribution.applyAsInt(old)));
  }
}
