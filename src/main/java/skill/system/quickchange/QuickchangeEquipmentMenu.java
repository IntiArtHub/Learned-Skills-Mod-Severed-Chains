package skill.system.quickchange;

import legend.core.GameEngine;
import legend.game.combat.bent.PlayerBattleEntity;
import legend.game.combat.ui.BattleHud;
import legend.game.combat.ui.ListMenu;
import legend.game.combat.ui.ListPosition;
import legend.game.i18n.I18n;
import legend.game.inventory.Equipment;
import legend.game.inventory.screens.FontOptions;
import legend.game.inventory.screens.TextColour;
import legend.game.scripting.RunningScript;
import legend.game.types.EquipmentSlot;
import legend.game.types.Renderable58;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static legend.game.Scus94491BpeSegment_800b.gameState_800babc8;
import static legend.game.Text.renderText;
import static legend.game.modding.coremod.CoreMod.INPUT_ACTION_MENU_BACK;
import static legend.game.modding.coremod.CoreMod.INPUT_ACTION_MENU_SORT;
import static legend.game.sound.Audio.playMenuSound;

/** A staged loadout editor. Browsing changes no equipment; confirmation commits once. */
final class QuickchangeEquipmentMenu extends ListMenu {
  private static final EquipmentSlot[] SLOTS = EquipmentSlot.values();
  private final EnumMap<EquipmentSlot, Equipment> staged = new EnumMap<>(EquipmentSlot.class);
  private final Predicate<Map<EquipmentSlot, Equipment>> confirm;
  private final FontOptions font = new FontOptions().colour(TextColour.WHITE);
  private EquipmentSlot browsingSlot;
  private List<Equipment> candidates = List.of();

  QuickchangeEquipmentMenu(final BattleHud hud, final PlayerBattleEntity player,
                           final ListPosition position,
                           final Predicate<Map<EquipmentSlot, Equipment>> confirm, final Runnable onClose) {
    super(hud, player, 290, position, onClose);
    this.confirm = confirm;
    this.resetPosition();
  }

  @Override protected int getListCount() {
    return this.browsingSlot == null ? SLOTS.length + 1 : this.candidates.size() + 1;
  }

  @Override protected void drawListEntry(final int index, final int x, final int y, final int trim) {
    this.font.trim(trim);
    if(this.browsingSlot == null) {
      if(index == SLOTS.length) {
        renderText(this.staged.isEmpty() ? "Confirm (select gear first)" : "Confirm loadout (" + this.staged.size() + ")", x, y + 3, this.font);
        return;
      }
      final EquipmentSlot slot = SLOTS[index];
      final Equipment item = this.staged.getOrDefault(slot, this.player_08.character.getEquipment(slot));
      this.drawIcon(item, x, y);
      renderText(label(slot) + ": " + (item == null ? "Empty" : I18n.translate(item.getNameTranslationKey())) +
        (this.staged.containsKey(slot) ? " *" : ""), x + 19, y, this.font);
    } else if(index == this.candidates.size()) {
      renderText("Back to loadout", x, y + 3, this.font);
    } else {
      final Equipment item = this.candidates.get(index);
      this.drawIcon(item, x, y);
      renderText(I18n.translate(item.getNameTranslationKey()), x + 19, y, this.font);
    }
  }

  private void drawIcon(final Equipment item, final int x, final int y) {
    if(item == null) return;
    final Renderable58 icon = item.renderIcon(x, y, Renderable58.FLAG_DELETE_AFTER_RENDER);
    if(icon != null) {
      // The battle list uses 14-pixel rows. Twelve-pixel icons leave a visible gap.
      icon.widthScale = 0.75f;
      icon.heightScale_38 = 0.75f;
      // Item icons default to menu depth 36 (z=144), behind the battle list's z=124 box.
      icon.z_3c = 30.0f;
    }
  }

  @Override public void tick() {
    if(this.menuState_00 == 2 && this.browsingSlot != null &&
      GameEngine.PLATFORM.isActionPressed(INPUT_ACTION_MENU_BACK.get())) {
      playMenuSound(3);
      this.showLoadout();
      return;
    }
    if(this.menuState_00 == 2 && GameEngine.PLATFORM.isActionPressed(INPUT_ACTION_MENU_SORT.get())) {
      gameState_800babc8.equipment_1e8.sort(Comparator
        .comparingInt((Equipment item) -> item.slot.ordinal())
        .thenComparing(item -> I18n.translate(item.getNameTranslationKey())));
      if(this.browsingSlot != null) this.refreshCandidates();
      playMenuSound(2);
      return;
    }
    // ListMenu normally deallocates after one selection. Keep it open while staging.
    if(this.menuState_00 == 6) {
      this.onUse(this.listScroll_1e + this.listIndex_24);
      return;
    }
    super.tick();
  }

  @Override public void draw() {
    super.draw();
    if(this.menuState_00 == 9) return;
    final int selected = this.listScroll_1e + this.listIndex_24;
    final EnumMap<EquipmentSlot, Equipment> preview = new EnumMap<>(this.staged);
    if(this.browsingSlot != null && selected < this.candidates.size()) {
      final Equipment choice = this.candidates.get(selected);
      if(choice == this.player_08.character.getEquipment(this.browsingSlot)) preview.remove(this.browsingSlot);
      else preview.put(this.browsingSlot, choice);
    }
    int attack = 0, defence = 0, magicAttack = 0, magicDefence = 0, speed = 0;
    for(final Map.Entry<EquipmentSlot, Equipment> entry : preview.entrySet()) {
      final Equipment old = this.player_08.character.getEquipment(entry.getKey());
      final Equipment next = entry.getValue();
      attack += next.attack_10 - old.attack_10;
      defence += next.defence_12 - old.defence_12;
      magicAttack += next.magicAttack_11 - old.magicAttack_11;
      magicDefence += next.magicDefence_13 - old.magicDefence_13;
      speed += next.speed_0f - old.speed_0f;
    }
    renderText("AT " + signed(attack) + "  DF " + signed(defence) + "  SPD " + signed(speed), 20, 152, this.font.trim(0));
    renderText("MAT " + signed(magicAttack) + "  MDF " + signed(magicDefence) + "    Y: Sort", 20, 166, this.font);
  }

  private static String signed(final int value) { return value >= 0 ? "+" + value : Integer.toString(value); }

  @Override protected void onSelection(final int index) { }

  @Override protected void onUse(final int index) {
    if(this.browsingSlot == null) {
      if(index == SLOTS.length) {
        if(this.staged.isEmpty()) {
          playMenuSound(40);
          this.resumeBrowsing();
        } else {
          if(this.confirm.test(Map.copyOf(this.staged))) {
            this.selectionState_a0 = 1;
            this.menuState_00 = 9;
          } else {
            playMenuSound(40);
            this.resumeBrowsing();
          }
        }
        return;
      }
      this.browsingSlot = SLOTS[index];
      this.refreshCandidates();
      this.resetPosition();
      this.resumeBrowsing();
      return;
    }
    if(index < this.candidates.size()) {
      final Equipment chosen = this.candidates.get(index);
      if(chosen == this.player_08.character.getEquipment(this.browsingSlot)) this.staged.remove(this.browsingSlot);
      else this.staged.put(this.browsingSlot, chosen);
    }
    this.showLoadout();
  }

  private void refreshCandidates() {
    final List<Equipment> available = new ArrayList<>(gameState_800babc8.equipment_1e8);
    for(final Map.Entry<EquipmentSlot, Equipment> entry : this.staged.entrySet()) {
      available.remove(entry.getValue());
      available.add(this.player_08.character.getEquipment(entry.getKey()));
    }
    final Equipment original = this.player_08.character.getEquipment(this.browsingSlot);
    final List<Equipment> choices = new ArrayList<>();
    for(final Equipment item : available) {
      if(item.slot == this.browsingSlot && QuickchangeEquipmentScope.canStage(this.player_08, original, item) &&
        !choices.contains(item)) choices.add(item);
    }
    if(this.staged.containsKey(this.browsingSlot)) choices.add(original);
    this.candidates = choices;
  }

  private void showLoadout() {
    this.browsingSlot = null;
    this.candidates = List.of();
    this.resetPosition();
    this.resumeBrowsing();
  }

  private void resetPosition() {
    this.listIndex_24 = 0;
    this.listScroll_1e = 0;
    this.listOffsetY_20 = this.listStartY_1a;
  }

  private void resumeBrowsing() {
    this.menuState_00 = 2;
    this.flags_02 &= ~0x4;
    this.flags_02 |= 0x1 | 0x2 | 0x8;
  }

  private static String label(final EquipmentSlot slot) {
    return switch(slot) {
      case WEAPON -> "Weapon";
      case HELMET -> "Helmet";
      case ARMOUR -> "Armour";
      case BOOTS -> "Boots";
      case ACCESSORY -> "Accessory";
    };
  }

  @Override protected void onClose() { }
  @Override protected int handleTargeting() { return 2; }
  @Override public void getTargetingInfo(final RunningScript<?> script) { }
}
