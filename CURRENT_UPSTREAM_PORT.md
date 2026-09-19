# Current-Upstream Port Evidence

Historical v1.0.0 port record. The v1.1.0 candidate was tested on build 1779; see the root README and release checklist for current status.

Frozen upstream: official Windows devbuild `3.0.0-1739-devbuild`, commit `b69c397a2ccb6165e12480d13523840073f08a80`.

## Binary verification

- Official archive SHA-256: `A2CBE418E45905AB4D2B80107ACD3618D018799233F62679C4C782DF98A5FA35`.
- Compiled engine filename: `lod-game-b69c397a2ccb6165e12480d13523840073f08a80.jar`.
- Compiled engine SHA-256: `724ABAB2BCE535421C49CA7CD2C0202963378531B52AB2A659C46DA46F463F54`.
- `javap -constants legend.core.Version` confirms `3.0.0-1739-devbuild` and the full commit. Source placeholders were not treated as release metadata.

## Compatibility findings

- Combat dispatch: stock `Battle.scriptSetUpAndHandleCombatMenu` retains and ticks `BattleMenuStruct58.currentAction`, but its first selection frame still returns every action registry ID to the fixed retail player-script table. Unknown custom IDs fall through to `ACTION_NOOP` and consume the turn before the next Java tick. The core therefore wraps only script-function slot 160: opted-in custom actions rewind on that first frame, then use the stock tick lifecycle until completion or cancellation. Native actions remain direct retail delegate calls.
- Renderer/icons: core renderer classes moved from `legend.core.opengl`/`legend.core` to `legend.core.renderer`. Manual atlas imports were ported; the atlas API and persistent managed-renderable approach remain available.
- Saves: upstream moved to tagged V10 saves with V8/V9 migration. Learned Skills mastery remains a registered campaign config entry rather than an engine serializer field, preserving separation from the engine save schema.
- Main menu: stock `MainMenuScreen` still contains hidden placeholder positions but no extension event. The optional patch adds only a generic event, descriptor, More screen, conditional button, translations, and focused tests.
- Inventory/equipment/shop: registration and shop event surfaces used by Learned Skills still compile unchanged against stock 1739.
- Character identity: mastery continues to key by stable character registry ID; the current character-template model preserves registry identities.

## Patch rebase classification

The old More-menu patch applied structurally without conflicts to the menu/event/localisation code. The sole three-way conflict was generated `Version.java` metadata. It was resolved to the verified current compiled values and retained the new upstream `isMac()` method. No combat, renderer, save, shop, inventory, equipment, or character code is part of the engine patch.
