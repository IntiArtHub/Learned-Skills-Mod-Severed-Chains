# Learned Skills

Learned Skills is an extensible gameplay framework for [Severed Chains](https://github.com/Legend-of-Dragoon-Modding/Severed-Chains) that integrates new abilities into character-specific progression.

Individual abilities can exist independently as standalone mods, but Learned Skills can wrap them in Skill Manuals that players discover or buy, equip, train through gameplay, rank up, and permanently master. This lets multiple gameplay modifications share one coherent progression system. Version 1.0.0 includes **Steal** as the first bundled Learned Skill, with additional Manuals and abilities planned.

The framework keeps ability logic modular. Where the architecture permits, a developer can reuse or adapt an individual ability independently of the Manual and mastery presentation.

## Standard installation â€” Recommended

1. Download `Learned-Skills-v1.0.0.zip` from the [v1.0.0 release](https://github.com/IntiArtHub/Learned-Skills-Mod-Severed-Chains/releases/tag/v1.0.0).
2. Copy `learned-skills-1.0.0.jar` into the Severed Chains `mods` folder.
3. Launch the game and enable **Learned Skills** for the campaign if prompted.

Done. This supplies the complete gameplay system and requires no engine replacement.

**Tested against Severed Chains `3.0.0-1739-devbuild`.** Ordinary upstream updates may remain compatible unless they change APIs or behaviour used by the mod.

## First bundled skill: Steal

Buy a Steal Manual from the Forest item shop and equip it in the accessory slot. Steal then appears in that character's battle command bar. Successful uses build character-specific mastery; higher ranks improve the success rate and unlock boss stealing. At maximum mastery the manual is consumed and that character retains Steal permanently.

Enemy held resources are generated per battle. A successful Steal can obtain an item or gold and removes that held resource without changing the enemy's normal post-battle drop.

## Optional Menu Addon

The optional Menu Addon adds **Main Menu â†’ More â†’ Skills**, showing exact mastery progress, rank, **Learning now**, **Not learning**, **MASTERED**, and each Skill Effect. It appears as a separate mod; enable both **Learned Skills** and **Learned Skills Menu Addon**. The core gameplay mod works fully without it.

> [!WARNING]
> The Menu Addon is tied to Severed Chains `3.0.0-1739-devbuild`, commit `b69c397a2ccb6165e12480d13523840073f08a80`. Do **not** update Severed Chains while it is installed. Uninstall the Menu Addon **before** updating Severed Chains.

The addon replaces one core engine JAR; standard Learned Skills does not. QoL+ and other modified engine distributions may conflict. Its installer verifies all relevant hashes and refuses unknown engines. Its uninstaller will not blindly restore an obsolete engine if another update changed the active engine after installation.

Read [menu-addon/README.md](menu-addon/README.md) before installing it.

## Compatibility

The standard JAR uses normal Severed Chains mod-loader APIs and is not permanently locked to build 1739. Recheck compatibility after upstream changes. QoL+ and other engine forks are untested with the standard JAR.

The Menu Addon is exact-build locked and must not be installed over QoL+, another fork, or any already modified engine.

Internal registry and save identifiers remain under `skill_system` for compatibility with pre-1.0 development saves.

## Developers

- [Future skill integration](FUTURE_SKILL_DEVELOPMENT.md)
- [Current upstream port evidence](CURRENT_UPSTREAM_PORT.md)
- [Generic engine patch](ENGINE_MENU_EXTENSION.patch)
- [Exact engine baseline](ENGINE_BASE.txt)
- [Animation research](ANIMATION_RESEARCH.md)
- [UI extension research](UI_EXTENSION_RESEARCH.md)

The generic engine source corresponding to the optional payload is published at [IntiArtHub/Severed-Chains, branch learned-skills-menu-v1.0.0](https://github.com/IntiArtHub/Severed-Chains/tree/learned-skills-menu-v1.0.0), exact commit [$commit](https://github.com/IntiArtHub/Severed-Chains/commit/3fd2f3879da16f687f0d6bfaca46095f85c9eecd), based directly on upstream commit 69c397a2ccb6165e12480d13523840073f08a80.

### Build prerequisites

JDK 25 and a locally obtained official Severed Chains 1739 installation are required for compilation. Copy `gradle.properties.example` to `gradle.properties` and set the local engine JAR/library paths. These proprietary/runtime dependencies are intentionally not committed, so a GitHub Actions workflow is not supplied.

Run `gradle clean check` for the stock-compatible core. The optional addon additionally requires the exact patched engine JAR through `menuAddonEngineJar`.

## Documentation

See [Known Issues](KNOWN_ISSUES.md), [Roadmap](ROADMAP.md), [Changelog](CHANGELOG.md), and [Third-Party Notices](THIRD_PARTY_NOTICES.md).