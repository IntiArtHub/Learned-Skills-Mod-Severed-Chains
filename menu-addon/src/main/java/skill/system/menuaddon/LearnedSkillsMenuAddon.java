package skill.system.menuaddon;

import legend.core.lang.I18nText;
import legend.game.inventory.screens.MenuScreen;
import legend.game.modding.events.menu.ExtensionMenuEntry;
import legend.game.modding.events.menu.GatherExtensionMenuEntriesEvent;
import org.legendofdragoon.modloader.Mod;
import org.legendofdragoon.modloader.ModContainer;
import org.legendofdragoon.modloader.events.EventListener;
import org.legendofdragoon.modloader.registries.RegistryId;

import static legend.core.GameEngine.EVENTS;
import static legend.core.GameEngine.MODS;

/** Exact-build adapter that contributes Learned Skills to the optional More menu. */
@Mod(id = LearnedSkillsMenuAddon.MOD_ID, version = "3.0.0")
public final class LearnedSkillsMenuAddon {
  public static final String MOD_ID = "learned_skills_menu_addon";
  private static final String CORE_MOD_ID = "skill_system";
  private static final String SKILLS_SCREEN_CLASS = "skill.system.menu.SkillsScreen";
  private static final RegistryId SKILLS_MENU_ID = new RegistryId(CORE_MOD_ID, "skills");

  public LearnedSkillsMenuAddon() {
    EVENTS.register(this);
  }

  @EventListener
  public void gatherExtensionMenuEntries(final GatherExtensionMenuEntriesEvent event) {
    final ModContainer core = MODS.getLoadedMods().stream()
      .filter(mod -> CORE_MOD_ID.equals(mod.modId))
      .findFirst()
      .orElse(null);
    if(core == null) return;

    event.add(new ExtensionMenuEntry(
      SKILLS_MENU_ID,
      new I18nText("skill_system.ui.skills"),
      100,
      unload -> this.createSkillsScreen(core, unload)
    ));
  }

  private MenuScreen createSkillsScreen(final ModContainer core, final Runnable unload) {
    try {
      final Class<?> screenClass = Class.forName(SKILLS_SCREEN_CLASS, true, core.getClassLoader());
      return (MenuScreen)screenClass.getConstructor(Runnable.class).newInstance(unload);
    } catch(final ReflectiveOperationException | ClassCastException ex) {
      throw new IllegalStateException("Unable to open the Learned Skills screen from the enabled core mod", ex);
    }
  }
}