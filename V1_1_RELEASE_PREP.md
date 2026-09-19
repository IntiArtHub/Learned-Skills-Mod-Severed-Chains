# v1.1.0 release preparation

Status: local candidate only. Do not push, tag, upload, or publish until the remaining acceptance checks pass and publication is explicitly approved.

## Included changes

- Steal remains available with its existing mastery and reward behaviour.
- Buff Dance and Quickchange add Manuals, cumulative mastery ranks, battle actions, and menu descriptions.
- Quickchange stages up to five equipment slots, sorts inventory with the menu sort action, shows net gear stat changes, and commits one loadout per use. Its completion plays animation 3, then animation 31 for eight ticks, and restores the prior animation.
- The Forest item shop adds the three Manuals and Bastard Sword at 30G. No other development gear is added.
- The animation viewer, Quickchange probes, runtime mastery and Steal overrides, and debug launcher have been removed from the distributable core.

## Verified locally

- Core clean build, tests, and stock compatibility check: passed.
- Core compiled and passed its full tests and stock compatibility check against the exact 1780 engine and libraries.
- Optional Menu Addon compiled and passed its isolation and engine extension tests against the exact 1780 patched engine payload. Its private installer hashes were refreshed for testing.
- The core JAR contains no animation viewer, Quickchange probe, or test-override classes.
- The earlier five-slot editor, sorting, stat preview, normal mastery gain, and animation choice were accepted in gameplay before the final spacing and cleanup edits.

## Before publication

- Visually retest the final icon size, entry spacing, back/confirm row spacing, and animation 3 to brief 31 sequence.
- Test Quickchange Manual consumption when mastery completes after the Manual has been swapped into inventory. Preserve a save that can be restored if needed.
- Retest the optional 1780 Menu Addon in game, including More > Skills and uninstall/restore behaviour, before deciding whether to include it as a separate exact-build release component.
- Audit the final package contents, hashes, license notices, and Git diff; confirm no local paths, saves, logs, debug tools, or private test kit enter the published archive.
- Confirm the target GitHub branch/tag and request explicit approval before any push, tag, release creation, or upload.
