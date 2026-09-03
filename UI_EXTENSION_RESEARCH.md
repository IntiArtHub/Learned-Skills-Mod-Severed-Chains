# Skills/menu UI extension reconnaissance

Target engine: `75cfb7462ca3d015e95e1e509a30458ca38bc278`.

The reconnaissance below records why the exact retail build had no safe extension seam. The approved narrow implementation is now captured in `ENGINE_MENU_EXTENSION.patch`.

## Exact-build menu lifecycle

- `Menus.initInventoryMenu()` directly calls `initMenu(WhichMenu.RENDER_NEW_MENU, () -> new MainMenuScreen(...))` (`Menus.java:81-86`). There is no factory registry or replacement event.
- `initMenu` clears managed renderables, applies the menu display size, sets `whichMenu_800bdc38`, and pushes the supplied screen (`Menus.java:65-78`). `loadAndRenderMenus` handles only the fixed `WhichMenu` states and calls `menuStack.render()` (`Menus.java:89-116`).
- `MenuStack.pushScreen` assigns the stack and installs global window input handlers when the first screen is pushed. `popScreen` detaches/deletes the top screen and removes handlers when empty (`MenuStack.java:49-66`).
- Rendering walks lower screens first only when the top screen's `propagateRender()` permits it. Input walks the top screen first only when `propagateInput()` permits it (`MenuStack.java:76-127`). Both default to `false` in `MenuScreen` (`MenuScreen.java:478-484`).
- `MenuScreen` owns control focus, hover, hotkeys and deferred actions. Input is offered to the screen/control tree and then its focused control. This is an internal screen framework, not a screen registry.
- `MainMenuScreen` owns `private final List<Button> menuButtons` and `private Button addButton(...)` (`MainMenuScreen.java:70,141`). Its constructor creates every native button, and each child route uses its private `showScreen` helper to push a screen whose unload callback pops it (`MainMenuScreen.java:93-138,516-596`).

## Supported hooks found

A complete search of event posting in `Menus`, `legend.game.inventory`, and `legend.game.inventory.screens` found inventory/equipment-type, shop, item-use, and new-game events, but no:

- menu-screen-created or menu lifecycle event;
- main-menu/button/control gather event;
- replace-screen factory or screen registry;
- generic menu extension;
- injectable `WhichMenu` handler.

`InputPressedEvent`/`InputReleasedEvent` are global notifications posted by the platform. They are not cancelable, do not identify the active `MenuScreen`, and `MenuStack` exposes no top-screen accessor. They therefore cannot provide a safe discoverable button seam.

## Status and Dragoon architecture

- Main-menu Status and character-card confirmation both construct `StatusScreen` directly (`MainMenuScreen.java:361,516-522`).
- `StatusScreen` always renders the same stats/equipment layout and calls `renderCharacterSpells` in that layout (`StatusScreen.java:117-120`).
- Dragoon information is only a conditional region: `character.hasDragoon()` gates the unlocked spell list (`StatusScreen.java:123-174`).
- Left/right input changes character. Up/down scrolls the spell list (`StatusScreen.java:237-259`). There is no page enum, alternate Status subclass, script-selected screen, or second-page push.
- `WhichMenu` contains only general render/unload/save/char-swap/quit states and no Status or Dragoon page state.

Conclusion: the remembered Dragoon information is the conditional spell panel inside the one Status screen, not a deeper retail page mechanism.

## QoL+ v1.20.7 inspection

Public release: `https://github.com/FrancisDionne/Severed-Chains/releases/tag/experimental`.

- The release identifies commit `6f278a4` and explicitly describes QoL+ as an altered Severed Chains build, not a typical mod.
- A source snapshot of public commit `6f278a4` contained no Bestiary, Statistics, or Archives implementation, so the requested binary fallback was necessary.
- Inspected Windows asset: `QoL-Build-v1.20.7-Windows-x64.zip`.
- Inspected jar: `lod-game-snapshot.jar`, SHA-256 `1F7EE7C24480A374A8651677189DCFE4486F69484ED1DCA56415323B3E0D96EC`.
- Decompiled/inspected classes: `legend.game.inventory.screens.MainMenuScreen`, `BestiaryScreen`, `StatisticsScreen`, `EquipmentScreen`, and `StatusScreen`; model classes include `legend.game.statistics.Bestiary`, `BestiaryEntry`, and `Statistics`.

QoL+ directly modifies core `MainMenuScreen`:

- It adds private `archivesButton`, `goodsButton`, `statsButton`, and `bestiaryButton` fields plus an internal integer `state`.
- `setState(0)` creates the ordinary menu with an `Archives` button. `setState(1)` disables/hides ordinary entries and creates `Goods`, `Bestiary`, and `Statistics` entries at fixed indices.
- `showArchives()` toggles that internal state in place.
- `showStatisticsScreen()` and `showBestiaryScreen()` pass constructor references for `StatisticsScreen(Runnable)` and `BestiaryScreen(Runnable)` to the same private `showScreen` helper, which calls `MenuStack.pushScreen`.

This is a fork-only core-class edit. It does not reveal or use a reusable mod extension seam.

QoL+ also does not solve `Already Learned`: its `EquipmentScreen.getEquippableItemsForCharacter` still calls the fork's `SItem.canEquip` before adding an item to the selectable list. `menuSelect` only sees accepted entries and equips them directly. There is no denial-reason callback or rejected-selection message.

## Feasibility assessment

### Top-level Main Menu Skills

Not cleanly possible in this target build from a normal mod. The desired position, button list, creation helper, and screen factories are all private and the root screen is hard-constructed. Reflection or bytecode transformation would be required.

### Skills adjacent to Status

A mod can implement its own `MenuScreen`, but cannot attach it beside Status without replacing/intercepting the hard-coded `StatusScreen` factory or mutating a live screen. Status has no page extension list and no lifecycle event. This is no cleaner than adding a top-level button.

### Mod-side bridge

A battle-style narrow function-slot bridge is not available here. The battle bridge wraps a public overlay script-function array; menu construction has no equivalent public indirection. A menu bridge would have to use reflection to access `MenuStack.screens`, `MainMenuScreen.menuButtons`, private `addButton`, or transform static bytecode. It would be sensitive to field names, constructor order, navigation math, focus, cleanup and other UI mods, so it is not recommended.

### Registered input action

Technically possible only as a fallback hotkey: register an action, listen globally, infer that the relevant menu is active, and push a Skills screen. Risks are non-cancelable duplicate input, no public active-screen identity, opening over the wrong `RENDER_NEW_MENU` screen, controller-binding conflicts, and poor discoverability. Safe gating would itself tend toward reflection or additional engine state.

## Smallest clean engine APIs

1. Add a generic `GatherMainMenuButtonsEvent` fired while `MainMenuScreen` builds its descriptor list. Each entry should carry a stable ID, localized `TextComponent`, ordering/anchor metadata, visibility/enabled state, and a screen factory or activation callback. `MainMenuScreen` should remain responsible for creating `Button` controls and navigation. This supports Skills, Bestiary, Statistics and other mods without exposing private controls.
2. For `Already Learned`, separate visibility from eligibility. A generic equipment-choice event/result should carry `visible`, `enabled`, and an optional localized rejection reason. The equipment list can retain a denied manual and `menuSelect` can display the reason without mutating equipment. This preserves the existing `canEquip` denial as the final safety check.

A generic all-screen-created event is less desirable than the focused button event: it would expose partially constructed screens and encourage mods to mutate private layout/control state.

## Ranked recommendation

1. Generic main-menu button descriptor event in the engine; then attach the existing Skills presentation through a normal `MenuScreen` factory.
2. A similarly generic Status-page descriptor event, only if the engine wants Status to become an explicit tab/page host.
3. Temporary registered hotkey, only if a discoverable button can be deferred and the engine adds an active-screen query/cancelable input contract.
4. Reflection/bytecode mod-side bridge.
5. Maintain a custom Severed Chains fork like QoL+.

This pre-implementation recommendation was reviewed and superseded by the approved narrow extension API below.

## Implemented narrow extension API

Base commit: `75cfb7462ca3d015e95e1e509a30458ca38bc278`.

Historical disposable worktree: local-only and not part of the published repository.

The patch changes exactly these engine files:

- `src/main/java/legend/core/Version.java`: verified release-build metadata matching pristine target build 1728.

- `src/main/java/legend/game/modding/events/menu/ExtensionMenuEntry.java`: generic stable descriptor and child screen factory.
- `src/main/java/legend/game/modding/events/menu/GatherExtensionMenuEntriesEvent.java`: duplicate-safe collection and deterministic sorting.
- `src/main/java/legend/game/modding/events/menu/package-info.java`: package nullability default.
- `src/main/java/legend/game/inventory/screens/MainMenuScreen.java`: conditional slot-11 `More` button while retaining the 14-position retail layout.
- `src/main/java/legend/game/inventory/screens/ExtensionMenuScreen.java`: native generic scrolling submenu and one-level navigation.
- `src/main/resources/lod_core/lang/en.lang`: engine-owned `More`/Back strings.
- `src/test/java/legend/game/inventory/screens/ExtensionMenuTest.java`: focused layout, ordering, duplicate, capacity, factory, and callback tests.
- `build.gradle`: opt-in `menuExtensionTest` task without enabling the excluded end-to-end suite.

The patch adds only generic engine infrastructure:

- `ExtensionMenuEntry`: stable `RegistryId`, localized `TextComponent`, integer order, and `Function<Runnable, ? extends MenuScreen>` factory.
- `GatherExtensionMenuEntriesEvent`: rejects duplicate IDs and returns an immutable order-then-ID sorted snapshot.
- `MainMenuScreen`: posts the gather event, keeps all 14 positions, leaves the first blank at index 5 hidden, and replaces only the second blank at index 11 with `More` when entries exist.
- `ExtensionMenuScreen`: engine-owned native panel/list, seven visible scrolling rows, keyboard/controller/mouse selection, and one-level child/back callbacks.
- `lod_core` localization and focused `ExtensionMenuTest` coverage.

Skill System registers `skill_system:skills` as a normal listener. All skill state and rendering remain mod-owned. The patch is source-compatible with consumers only after the engine API is present; this mod build does not load on unpatched `75cfb746` because its listener and screen descriptor directly reference the new engine classes.
## Beta UI correction evidence

The two small `::` artefacts visible at the bottom of More and the Skills list were the Main Menu timestamp separator renderables allocated at logical positions `(146, 184)` and `(164, 184)`. Control-based child screens stopped Main Menu rendering but did not deallocate these already-created legacy glyphs. `ExtensionMenuScreen` now performs the standard one-time `deallocateRenderables(0xff)` / `deallocateRenderables(0)` cleanup before drawing, removing the source rather than covering it.

Skills detail bodies now use measured word wrapping against the active native font. Effect and Description headings stay at native scale; their bodies have an explicit 152-logical-pixel maximum width and a modest `0.8` scale. They remain separate localized regions, with explicit effect line breaks preserved and independently width-checked.

The engine patch also carries the verified target release metadata from the pristine jar (`3.0.0-1728-devbuild`) because plain local Gradle leaves upstream `Version.java` placeholders unresolved.