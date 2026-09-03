package skill.system.menu;

import legend.core.lang.I18nText;
import legend.core.lang.RawText;
import legend.core.platform.input.InputAction;
import legend.game.characters.CharacterData2c;
import legend.game.i18n.I18n;
import legend.game.inventory.screens.HorizontalAlign;
import legend.game.inventory.screens.InputPropagation;
import legend.game.inventory.screens.MenuScreen;
import legend.game.inventory.screens.TextColour;
import legend.game.inventory.screens.controls.Background;
import legend.game.inventory.screens.controls.CharacterCard;
import legend.game.inventory.screens.controls.Label;
import legend.game.inventory.screens.controls.ListBox;
import legend.game.inventory.screens.controls.Panel;
import skill.system.SkillSystemRuntime;
import skill.system.api.SkillDefinition;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static legend.game.Scus94491BpeSegment_800b.characterIndices_800bdbb8;
import static legend.game.Scus94491BpeSegment_800b.gameState_800babc8;
import static legend.game.modding.coremod.CoreMod.INPUT_ACTION_MENU_BACK;
import static legend.game.modding.coremod.CoreMod.INPUT_ACTION_MENU_LEFT;
import static legend.game.modding.coremod.CoreMod.INPUT_ACTION_MENU_RIGHT;
import static legend.game.sound.Audio.playMenuSound;

/** Character-specific, mod-owned skill mastery and effect screen. */
public final class SkillsScreen extends MenuScreen {
  static final int DETAIL_TEXT_WIDTH = 152;
  static final float DETAIL_BODY_SCALE = 0.8f;

  private final Runnable unload;
  private final CharacterCard characterCard;
  private final ListBox<SkillScreenModel.Entry> skills;
  private final Label emptyMessage;
  private final Label selectedName;
  private final Label status;
  private final Label mastery;
  private final Label effectBody;
  private final Label descriptionBody;
  private int characterSlot;
  private boolean allowWrapX = true;

  public SkillsScreen(final Runnable unload) {
    this.unload = unload;
    this.addControl(new Background());

    final Panel listPanel = this.addControl(Panel.panel());
    listPanel.setPos(8, 82);
    listPanel.setSize(174, 126);
    final Panel detailPanel = this.addControl(Panel.panel());
    detailPanel.setPos(188, 18);
    detailPanel.setSize(172, 190);

    this.characterCard = this.addControl(new CharacterCard());
    this.characterCard.setPos(8, 12);

    final Label title = this.addControl(new Label(new I18nText("skill_system.ui.skills")));
    title.setPos(197, 27);
    title.setSize(154, 14);
    title.getFontOptions().horizontalAlign(HorizontalAlign.CENTRE);

    this.skills = this.addControl(new ListBox<>(entry -> I18n.translate(entry.skill().nameTranslationKey()) + "   Lv. " + entry.rank(), null, null, null, null));
    this.skills.setPos(10, 86);
    this.skills.setSize(168, 119);
    this.skills.onHighlight(this::showDetails);
    this.skills.onSelection(this::showDetails);

    this.emptyMessage = this.addControl(new Label(new I18nText("skill_system.ui.no_skills")));
    this.emptyMessage.setPos(20, 104);

    this.selectedName = label(198, 48, DETAIL_TEXT_WIDTH, 14);
    this.status = label(198, 70, DETAIL_TEXT_WIDTH, 14);
    this.mastery = label(198, 91, DETAIL_TEXT_WIDTH, 14);
    label(new I18nText("skill_system.ui.effect"), 198, 118, DETAIL_TEXT_WIDTH, 14);
    this.effectBody = label(198, 134, DETAIL_TEXT_WIDTH, 27);
    this.effectBody.setScale(DETAIL_BODY_SCALE);
    label(new I18nText("skill_system.ui.description"), 198, 164, DETAIL_TEXT_WIDTH, 14);
    this.descriptionBody = label(198, 180, DETAIL_TEXT_WIDTH, 28);
    this.descriptionBody.setScale(DETAIL_BODY_SCALE);

    this.addHotkey(new I18nText("skill_system.ui.back"), INPUT_ACTION_MENU_BACK, this::back);
    this.setFocus(this.skills);
    this.loadCharacter(0);
  }

  private Label label(final int x, final int y, final int width, final int height) {
    return this.label(RawText.BLANK, x, y, width, height);
  }

  private Label label(final legend.core.lang.TextComponent text, final int x, final int y, final int width, final int height) {
    final Label label = this.addControl(new Label(text));
    label.setPos(x, y);
    label.setSize(width, height);
    return label;
  }

  private CharacterData2c currentCharacter() {
    return gameState_800babc8.charData_32c.get(characterIndices_800bdbb8.getInt(this.characterSlot));
  }

  private void loadCharacter(final int slot) {
    this.characterSlot = slot;
    final CharacterData2c character = this.currentCharacter();
    this.characterCard.setCharacter(character);

    final Map<String, Integer> mastery = new HashMap<>();
    final Set<String> equippedManuals = new HashSet<>();
    for(final SkillDefinition skill : SkillSystemRuntime.skills()) {
      mastery.put(skill.id(), SkillSystemRuntime.getSkillMastery(character, skill));
      if(SkillSystemRuntime.isSkillManualEquipped(character, skill)) equippedManuals.add(skill.manualId());
    }

    final List<SkillScreenModel.Entry> entries = SkillScreenModel.entries(SkillSystemRuntime.skills(), mastery, equippedManuals);
    this.skills.removeIf(ignored -> true);
    entries.forEach(this.skills::add);
    this.emptyMessage.setVisibility(entries.isEmpty());
    this.skills.setVisibility(!entries.isEmpty());
    if(entries.isEmpty()) {
      this.clearDetails();
    } else {
      this.skills.select(0);
      this.showDetails(entries.getFirst());
    }
  }

  private void showDetails(final SkillScreenModel.Entry entry) {
    this.selectedName.setText(new RawText(I18n.translate(entry.skill().nameTranslationKey()) + "   Lv. " + entry.rank()));
    this.status.setText(new RawText(I18n.translate(switch(entry.state()) {
      case LEARNING_NOW -> "skill_system.ui.learning_now";
      case NOT_LEARNING -> "skill_system.ui.not_learning";
      case MASTERED -> "skill_system.ui.mastered";
    })));
    this.status.getFontOptions().colour(switch(entry.state()) {
      case LEARNING_NOW -> TextColour.CYAN;
      case NOT_LEARNING -> TextColour.GREY;
      case MASTERED -> TextColour.GOLD;
    });
    this.mastery.setText(new RawText(I18n.translate("skill_system.ui.mastery") + "  " + entry.progress()));

    final String effects = entry.effectTranslationKeys().stream().map(I18n::translate).collect(Collectors.joining("\n"));
    this.effectBody.setText(new RawText(wrap(this.effectBody, effects)));
    this.descriptionBody.setText(new RawText(wrap(this.descriptionBody, I18n.translate(entry.skill().descriptionTranslationKey()))));
  }

  private static String wrap(final Label label, final String text) {
    return TextWrap.wrap(text, DETAIL_TEXT_WIDTH,
      value -> Math.round(label.getFont().textWidth(value) * label.getScale()));
  }

  private void clearDetails() {
    this.selectedName.setText(RawText.BLANK);
    this.status.setText(RawText.BLANK);
    this.mastery.setText(RawText.BLANK);
    this.effectBody.setText(RawText.BLANK);
    this.descriptionBody.setText(RawText.BLANK);
  }

  private void navigate(final int delta) {
    final int count = characterIndices_800bdbb8.size();
    if(count <= 1) return;
    final int next = Math.floorMod(this.characterSlot + delta, count);
    if(next != this.characterSlot) {
      playMenuSound(1);
      this.loadCharacter(next);
    }
  }

  private void back() {
    playMenuSound(3);
    this.unload.run();
  }

  @Override protected void render() { }

  @Override
  protected InputPropagation inputActionPressed(final InputAction action, final boolean repeat) {
    if(super.inputActionPressed(action, repeat) == InputPropagation.HANDLED) return InputPropagation.HANDLED;
    if(action == INPUT_ACTION_MENU_LEFT.get()) {
      if(!repeat || this.allowWrapX) this.navigate(-1);
      this.allowWrapX = false;
      return InputPropagation.HANDLED;
    }
    if(action == INPUT_ACTION_MENU_RIGHT.get()) {
      if(!repeat || this.allowWrapX) this.navigate(1);
      this.allowWrapX = false;
      return InputPropagation.HANDLED;
    }
    if(action == INPUT_ACTION_MENU_BACK.get() && !repeat) {
      this.back();
      return InputPropagation.HANDLED;
    }
    return InputPropagation.PROPAGATE;
  }

  @Override
  public InputPropagation inputActionReleased(final InputAction action) {
    if(super.inputActionReleased(action) == InputPropagation.HANDLED) return InputPropagation.HANDLED;
    if(action == INPUT_ACTION_MENU_LEFT.get() || action == INPUT_ACTION_MENU_RIGHT.get()) {
      this.allowWrapX = true;
      return InputPropagation.HANDLED;
    }
    return InputPropagation.PROPAGATE;
  }
}
