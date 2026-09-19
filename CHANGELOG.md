# Changelog

## 1.1.0 release candidate (local, unpublished)

- Added Buff Dance and Quickchange alongside Steal, using cumulative mastery thresholds of 20 / 60 / 99.
- Added Buff Dance party stat effects and rank-scaled Quickchange uses.
- Added Quickchange's staged five-slot loadout editor with equipment icons, names, net gear deltas, sorting, cancellation, and one final confirmation.
- Quickchange completion now plays animation 3, briefly transitions through animation 31, then restores the previous animation.
- Added the v1.1 menu icon spacing and layering adjustments; final spacing and animation cleanup still need a last visual retest.
- Removed hidden Quickchange probes, the animation preview action, and test property overrides from the release candidate.
- Reduced Forest vendor test stock to the Bastard Sword at 30G; Broad Sword is ATK 2 and Bastard Sword is ATK 7 in the current test data.
- Revalidated the core against Severed Chains 3.0.0-1780-devbuild and ported the exact-build Menu Addon engine extension to commit `d3d4c02cbf1c74a4db39cfc10520431a6b8cc150`.
- The candidate has not been published.

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
