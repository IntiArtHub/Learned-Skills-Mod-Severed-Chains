# Steal action integration research

> Historical research record. Development-only animation preview and probe tooling described by older notes has been removed from the v1.1.0 release candidate; this file is retained for implementation evidence.

## Native enemy stealing

- Spring Hitter is retail monster ID 39; its patched combat script is `patches/scripts/DRGN1/40.diff`.
- Crafty Thief is retail monster ID 83; its patched combat script is `patches/scripts/DRGN1/84.diff`.
- Those patches replace stolen-item bookkeeping with registry IDs (`reg[10]`) and, for Crafty Thief, restore stolen gold. They do not isolate movement or animation into a reusable engine action.
- The run/contact/return behavior therefore remains embedded in each full enemy script. Reusing it requires full script disassembly, not calling a Java Steal primitive.

### Full-script evidence

The pristine backup binaries were decompiled with the installed
`script-recompiler-0.7.11` and its local `snapshot` metadata. No output was
written into the game copy.

- Spring Hitter (`DRGN1.BIN/40`) steal entry calls `LABEL_82`. At `0x3228` it
  saves the actor position; `0x32ac` faces the target; `0x32b8` loads animation
  2; and `0x32c4` moves to target-relative X `-0xaf0` over 20 ticks. Its contact
  portion switches to animation `0x10` at `0x3378` and uses enemy-specific
  sounds/effects. `LABEL_86` restores idle animation 0 and moves to the saved
  absolute XYZ over 3 ticks (`0x3568`), then sets a fixed enemy-facing rotation.
- Crafty Thief (`DRGN1.BIN/84`) saves origin through `LABEL_156` (`0x5124`).
  `LABEL_158` faces the target, loads animation 2, and moves to target-relative
  X `-0x3e8` over 15 ticks (`0x5184-0x519c`). Its steal contact uses animation
  `0x12` (`0x2f8c`) plus a bespoke billboard/particle script (`LABEL_81`).
  `LABEL_159` moves to the saved absolute XYZ over 3 ticks (`0x5204`) and then
  restores idle animation 0 (`0x5230`).
- Both routines use the same safe structural primitives: save absolute origin,
  face target, movement animation 2, target-relative timed approach, bespoke
  contact, absolute timed return, idle restoration. Animation `0x10`/`0x12`
  and the attached effects are enemy-model-specific and cannot safely be loaded
  into arbitrary player models.

The player implementation therefore explicitly requests player movement
animation 2 before both legs and keeps its loop flag enabled during movement.
It uses a 15-tick Java interpolation to a target-relative contact point. The
installed retail `throw_item.txt` identifies player animation 7 as the native
throw/item-use motion (`THROW_ATTACK_ITEM`, line 334), so Steal now uses that as
its short reach/contact approximation, with animation 8 only as a missing-asset
fallback. The contact animation is marked `FLAG_ANIMATE_ONCE` and resolution
waits for the model's real `remainingFrames_9e` to reach zero, bounded by a
60-tick safety timeout. This fixes the previous repeated animation-6 jump:
animation 6 is part of the standard addition/attack sequence, and the custom
loader had left the preceding loop state active.

Return still uses animation 2 over 3 ticks to the exact stored origin, then
restores the stored rotation, prior animation index, and prior loop state. This
mirrors the retail structure without importing monster assets or adding a
movement system. Its appearance and camera interaction remain human-test items.

## UI renderer evidence and fixes

### Final custom-icon root cause and replacement

- The deleted synthetic `ManualItemIcon` did build and assign a non-null
  `UiType.obj`; `SItem.buildUiRenderable` also assigned a valid `vertexStart`.
  `Menus.renderUi` reached its glyph queue, and the metrics `useTexture`
  override did bind the PNG. Registration timing, object construction and the
  packaged image were therefore not the failure.
- The synthetic path was nevertheless incompatible with an arbitrary RGBA
  texture. `SItem.buildUiRenderable` permanently writes retail PSX UI vertex
  semantics, including CLUT/VRAM fields and
  `Bpp.of(metrics.tpage_06 >>> 7 & 0b11)`. The old metrics used retail-style
  `tpage = 0x19`, which encodes 4-bpp indexed rendering. `Menus.renderUi` then
  applies tpage/clut overrides. Calling `QueuedModelStandard.texture(...)`
  from `useTexture` does not turn those vertices into a normalized 24-bpp RGBA
  quad.
- The original 1 x 1 `QuadBuilder` UV extent was actually correct for direct
  normalized RGBA sampling. Changing it to 64 x 64 was not a valid repair and
  compounded the mismatch; the PNG itself is a valid 64 x 64 RGBA resource.
- The atlas resource remains registered as
  `skill_system:steal_manual_icon` through `RegisterAtlasTexturesEvent`. The
  battle command still calls `TextureAtlasIcon.render(MV)` directly; that
  already-working renderer and its accepted position were not changed.
- Inventory surfaces now use `ManualAtlasItemIcon`, which builds one persistent
  `Bpp.BITS_24` quad with normalized packed-atlas UVs and a custom
  `RenderableMetrics14.useTexture` override. That metrics override binds the
  RGBA atlas texture and enables texture alpha without reintroducing retail
  4-bpp/tpage/CLUT vertex semantics.
- `ManualAtlasItemIcon.render` follows the native `ItemIcon` contract: it
  allocates a `Renderable58`, preserves caller flags, adds
  `FLAG_NO_ANIMATION`, and adds the result to the engine-managed renderable
  list. `renderManual` returns the same custom renderable without managing it.
  Consequently allocation-only callers retain the icon, while callers using
  `FLAG_DELETE_AFTER_RENDER` can recreate it each frame exactly like a retail
  icon.
- Exact-build lifetime evidence: `SItem.renderCharacterEquipment` calls
  `equipment.renderIcon` only while `allocate` is true, so the old immediate
  queue disappeared from Status and Equipment equipped summaries after that
  frame. `SItem.renderMenuItems`, `ShopExtension.drawShopRow`, and
  `ShopScreen.renderSellList` call it each rendered frame.
- The shop failure had a separate concrete render-order component. The direct
  atlas quad and native icon/UI glyphs used depth 144. Ortho equal-depth entries
  are reverse-sequence sorted (`RenderEngine.orthoTranslucencySorter`), while
  managed UI glyphs use `GL_LEQUAL` for opaque and translucent depth. The later
  immediate atlas draw could therefore be rendered first and then covered by
  earlier managed shop UI at equal depth. Main Inventory's `ItemList`
  background is at `z_3c = 80` (render depth 320), so it did not reproduce the
  same equal-depth occlusion. Moving the atlas quad into `Menus.renderUi`
  gives it the same managed depth/order semantics as native icons; no shop
  special case was added.
- The 64 x 64 PNG has visible alpha bounds from source pixel 4 through 58. Its
  managed 16 x 16 footprint remains native-sized. A manual-icon-only intrinsic
  x anchor of -3 logical pixels corrects the human-observed Inventory offset;
  the direct battle transform is unchanged.
- Debug-only diagnostics now log once per distinct call stack/flags context:
  runtime equipment class, registry ID, x/y, flags, atlas resolution and
  rectangle, and `drawPath=managed-renderable`. This distinguishes main
  Inventory, shop buy/sell, equipment candidate, Equipment equipped-summary,
  and Status equipped-summary calls when run with
  `-Dskill_system.debug=true`.

### Other verified UI fixes

- `UiBox.setZ(z)` queues the box near depth `z`, while `renderText` multiplies
  `textZ_800bdf00` by four. Steal follows the native `BattleHud` name-box path:
  default box depth, text depth 0, 0.8 font scale, and a width derived from the
  actual font line width. Human runtime testing confirms text renders correctly
  inside the box.
- Item success text uses the inventory entry's name translation key; human
  runtime testing confirms a stolen Healing Potion is named correctly.
- The manual uses a local two-line description (`Equip to use Steal.` /
  `Consumed when mastered.`); human runtime testing confirms it fits acceptably.

## Current 1739 runtime correction

Human core-only testing on official `3.0.0-1739-devbuild` (`b69c397a`) confirmed that upstream's retained `currentAction` tick path does not remove the first-frame dispatch gap. Function 160 stores the selected custom action and returns its registry ID immediately; the unchanged fixed player-script table maps that unknown ID to `ACTION_NOOP`, ending the turn before the next Java tick.

The restored mod-local bridge wraps only function slot 160. When a newly selected action implements `BridgedBattleAction`, it converts that first `CONTINUE` to `PAUSE_AND_REWIND`. Later calls use the stock 1739 tick lifecycle unchanged: paused ticks remain paused, cancellation clears the action and repeats the turn, and completion returns `CONTINUE` exactly once. Unmarked native actions return the retail delegate result unchanged. Human retesting confirmed Steal now reaches targeting and executes normally.
## Player action dispatch trace

Evidence is from the installed build `75cfb746`. The installed script is
`<Severed-Chains-install>\files\player_combat_script`
(SHA-256 `11711D043213C06CBE04CD6A571640FBB7E807A7C03E4AAB037167E8666B5CC4`);
its textual source is the matching devkit `patches/scripts/player_combat_script.txt`.

1. `BattleHud.initializeMenuIcons` posts `GatherBattleActionsEvent`, sorts its
   `BattleAction -> position` map, and stores the resulting objects in
   `BattleMenuStruct58.actions` (`BattleHud.java:1337-1347`).
2. Confirm input calls `BattleHud.useAction` (`BattleHud.java:1593-1596`). That
   method calls `BattleAction.use`; `FAIL` and `PAUSE_SCRIPT` return no selected
   action, while `CONTINUE_SCRIPT` returns the selected object
   (`BattleHud.java:1669-1687`). Additions deliberately uses `PAUSE_SCRIPT` to
   open its list and therefore is not a committed turn action.
3. Script function 160, `Battle.scriptSetUpAndHandleCombatMenu`, assigns that
   result to `battleMenu.currentAction` and writes its registry ID to the
   script's output register (`Battle.java:4032-4072`).
4. `HANDLE_BATTLE_HUD` in the player combat script calls function 160 and then
   initializes legacy handler index `stor[30]` to 9 (`player_combat_script.txt:
   4655-4663`).
5. A fixed comparison chain translates only `lod:attack`, `lod:guard`,
   `lod:items`, `lod:spells`, `lod:escape`, `lod:transform`, `lod:special`, and
   `lod:d_attack` into legacy handler indexes (`4667-4689`). This is a fixed
   registry-to-legacy translation table, not a registry lookup failure or ID
   collision.
6. Unmatched registry IDs leave index 9 intact. `gosub_table` dispatches index
   9 to `ACTION_NOOP`, whose body is only `return` (`4692`, `4701-4716`,
   `4745-4746`).
7. The HUD routine resets `var[45][253]` to zero before selection. Because the
   no-op handler does not change it, the post-handler branch hides the HUD and
   returns instead of repeating the turn (`4658`, `4693-4696`, `4730-4738`).
   Thus an unknown action consumed a turn without executing Java action logic.

## Implemented bridge

The engine exposes its active overlay script-function array as
`Scus94491BpeSegment_8004.engineStateFunctions_8004e29c`. On an
`EngineStateChangeEvent` into `Battle`, the mod wraps only function slot 160 and
retains the exact retail function as its delegate. It does not edit the player
script or the supplied game copy.

The distinction is explicit and generic:

- ordinary/native `BattleAction`: not marked `BridgedBattleAction`; call and
  return the retail function unchanged;
- Java-owned custom `BattleAction`: opts into `BridgedBattleAction`; after the
  retail function records the selection and registry ID, the wrapper rewinds
  that same script call while the action's `tick` returns `PAUSE_SCRIPT`.

`CONTINUE_SCRIPT` releases the player script exactly once. The already-recorded
custom registry ID then reaches the retail unknown-ID no-op, but only as a
post-completion turn-return trampoline; it no longer replaces the custom
action. `REPEAT_TURN` clears the selection and rewinds to the menu, supporting
target cancellation and pre-commit rejection without consuming the turn.
`IGNORE` is rejected for an opted-in action so it cannot silently fall through.

This is reusable for future Skill Manual actions such as Quickchange. The
bridge contains no Steal-specific condition, targeting rule, movement, reward,
or message logic.

## Bridge verification status

Automated tests exercise the real wrapper with a minimal custom probe action:

- Attack, Guard, Items, Additions, Spells, Transform/Dragoon, and Escape-shaped
  unmarked actions call the retail delegate and are never ticked by the bridge;
- a custom action persists through two paused ticks and completes on the third;
- the retail player function is not called again while Java owns the action;
- cancellation clears the selected action and repeats the turn;
- an invalid `IGNORE` lifecycle fails loudly rather than consuming a no-op turn.

Steal is now registered through `GatherBattleActionsEvent` and uses this bridge.
The expanded suite has 28 passing tests. Core Steal resolution, depletion,
mastery, bridge completion, exactly-once turn accounting, messages,
description fit, and contact animation have been live-verified. The new
mod-atlas icon requires the next controlled human visual pass.

No engine source, script format, script opcode, or supplied game file was
changed. The compatibility dependency is the public slot-160 overlay function
array used by build `75cfb746`; the installer fails immediately if that slot is
missing rather than applying a partial bridge.

## Boss registry caveat

The initial explicit ID set comes from confidently named boss encounter registrations. Some multi-part or trial encounters remain semantically ambiguous, including targetable weapons/parts and Shirley trial representations. They are currently treated conservatively as bosses and need gameplay review.

## Remaining UI hook boundaries

Static bytecode inspection of build `75cfb746` found two narrow engine API
gaps. `EquipmentScreen.getEquippableItemsForCharacter` calls
`CharacterData2c.canEquip` and omits denied equipment before selection, so a mod
can safely deny a mastered manual but cannot receive an attempted selection to
show `Already Learned`. `MainMenuScreen` owns a private `menuButtons` list and
private `addButton` method and posts no gather/extension event, so a mod cannot
add a supported Skills entry. The Skills presentation model is implemented and
tested, but attaching its screen requires a public main-menu button event/hook.

The smallest engine additions would be (1) an equipment visibility/rejection
event carrying a denial message, and (2) a gather-main-menu-buttons event or
public screen-registration hook. Neither affects combat scripts or opcodes.
