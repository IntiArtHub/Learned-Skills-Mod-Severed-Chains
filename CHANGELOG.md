# Changelog

## 1.0.0

- Released Learned Skills as an extensible Skill Manual and character-mastery framework, with Steal as the first bundled skill.
- Retained internal `skill_system` registry/save identifiers for compatibility.
- Ported to official Severed Chains `3.0.0-1739-devbuild` (`b69c397a`).
- Split distribution into a stock-compatible core JAR and an optional exact-build Menu Addon.
- Added the Steal Manual, battle command, item/gold stealing, rank improvements, boss unlock, permanent mastery, persistence, animation, messages, and custom icon.
- Added a generic bridge that lets opted-in custom Java battle actions complete before the fixed retail action table handles the turn.
- Added the optional **More → Skills** mastery screen and isolated-classloader-safe addon registration.
- Added hash-verifying Menu Addon installation, verification, backup, and safe uninstallation tools.
- Added stock-linkage and addon-isolation build guards.