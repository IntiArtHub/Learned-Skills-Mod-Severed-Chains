# Learned Skills

Learned Skills is an extensible gameplay framework for [Severed Chains](https://github.com/Legend-of-Dragoon-Modding/Severed-Chains) that integrates new abilities into character-specific progression.

Individual abilities can exist independently as standalone mods, but Learned Skills can wrap them in Skill Manuals that players discover or buy, equip, train through gameplay, rank up, and permanently master. This lets multiple gameplay modifications share one coherent progression system. The local v1.1.0 release candidate includes **Steal**, **Buff Dance**, and **Quickchange**.

The framework keeps ability logic modular. Where the architecture permits, a developer can reuse or adapt an individual ability independently of the Manual and mastery presentation.

## Standard installation Recommended

1. Use the locally prepared v1.1.0 release candidate package.
2. Copy the included v1.1.0 core JAR into the Severed Chains `mods` folder.
3. Launch the game and enable **Learned Skills** for the campaign if prompted.

Done. This supplies the complete gameplay system and requires no engine replacement.

**Tested in Severed Chains `3.0.0-1780-devbuild`.** The core also passes a stock-engine compile/linkage check. The v1.1.0 candidate has not been published. Ordinary upstream updates may remain compatible unless they change APIs or behaviour used by the mod.

## First bundled skill: Steal

Buy a Steal Manual from the Forest item shop and equip it in the accessory slot. Steal then appears in that character's battle command bar. Successful uses build character-specific mastery; higher ranks improve the success rate and unlock boss stealing. At maximum mastery the manual is consumed and that character retains Steal permanently.

Enemy held resources are generated per battle. A successful Steal can obtain an item or gold and removes that held resource without changing the enemy's normal post-battle drop.

## Buff Dance and Quickchange

**Buff Dance** raises ATK, M ATK, DEF, M DEF, and SPEED for the party for each recipient's next three turns. Its mastery thresholds are 20, 60, and 99, with rank strengths of 15%, 30%, 50%, and 75%.

**Quickchange** opens a staged five-slot loadout editor with equipment icons, names, net stat deltas, sorting, cancellation, and one final confirmation. Its mastery thresholds are 20, 60, and 99, with 1, 2, 3, and unlimited uses per battle. Completion starts animation 3, briefly plays animation 31, then restores the previous animation.

The Forest vendor keeps its normal stock and sells all three Skill Manuals. Bastard Sword is the only extra stock equipment added, at 30G, to give Dart an early Quickchange option. The stock equipment definitions list Broad Sword at ATK 2 and Bastard Sword at ATK 7.

## Optional Menu Addon

The optional Menu Addon adds **Main Menu â†’ More â†’ Skills**, showing exact mastery progress, rank, **Learning now**, **Not learning**, **MASTERED**, and each Skill Effect. It appears as a separate mod; enable both **Learned Skills** and **Learned Skills Menu Addon**. The core gameplay mod works fully without it.

> [!WARNING]
> The private Menu Addon test kit is tied to Severed Chains `3.0.0-1780-devbuild`, commit `d3d4c02cbf1c74a4db39cfc10520431a6b8cc150`. Do **not** update Severed Chains while it is installed. Uninstall the Menu Addon **before** updating Severed Chains.

The addon replaces one core engine JAR; standard Learned Skills does not. QoL+ and other modified engine distributions may conflict. Its installer verifies all relevant hashes and refuses unknown engines. Its uninstaller will not blindly restore an obsolete engine if another update changed the active engine after installation.

Read [menu-addon/README.md](menu-addon/README.md) before installing it.

## Compatibility

The standard JAR uses normal Severed Chains mod-loader APIs and is not permanently locked to build 1780. Recheck compatibility after upstream changes. QoL+ and other engine forks are untested with the standard JAR.

The Menu Addon is exact-build locked and must not be installed over QoL+, another fork, or any already modified engine.

Internal registry and save identifiers remain under `skill_system` for compatibility with pre-1.0 development saves.

## Developers

- [Future skill integration](FUTURE_SKILL_DEVELOPMENT.md)
- [Current upstream port evidence](CURRENT_UPSTREAM_PORT.md)
- [Generic engine patch](ENGINE_MENU_EXTENSION.patch)
- [Exact engine baseline](ENGINE_BASE.txt)
- [Animation research](ANIMATION_RESEARCH.md)
- [UI extension research](UI_EXTENSION_RESEARCH.md)

The optional 1780 Menu Addon source is retained in the private test kit. No v1.1.0 GitHub publication has been made.

### Build prerequisites

JDK 25 and locally obtained Severed Chains engine JARs/libraries are required for compilation. The core currently compiles against the stock 1739 API baseline and has also passed its complete build and compatibility checks against 1780. Copy `gradle.properties.example` to `gradle.properties` and set the local engine JAR/library paths. These runtime dependencies are intentionally not committed, so a GitHub Actions workflow is not supplied.

Run `gradle :check` for the stock-compatible core. The optional addon additionally requires the exact patched engine JAR through `menuAddonEngineJar`; run `gradle :menu-addon:check -PmenuAddonEngineJar=<path>` against the matching build.

## Documentation

See [v1.1 release preparation](V1_1_RELEASE_PREP.md), [Known Issues](KNOWN_ISSUES.md), [Roadmap](ROADMAP.md), [Changelog](CHANGELOG.md), and [Third-Party Notices](THIRD_PARTY_NOTICES.md).
