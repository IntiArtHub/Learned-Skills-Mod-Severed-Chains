# v1.1.0 release preparation

Status: accepted by the user on Severed Chains 3.0.0-1780-devbuild and explicitly approved for GitHub publication.

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
- The five-slot editor, sorting, stat preview, normal mastery gain, animation choice, final spacing, and overall release state were accepted by the user.

## Final publication record

- User accepted the current gameplay and interface state and explicitly authorised publication.
- The combined release ZIP contains the core JAR plus the optional Menu Addon installer, verifier, uninstaller, payloads, exact source patch, build instructions, notices, and AGPL licence.
- Core and Menu Addon builds passed against 1780. The public installer passed pristine verification, v1.0 filename upgrade, install, patched verification, uninstall, and pristine restoration in a disposable layout.
- Quickchange Manual consumption after moving the Manual into inventory remains documented in Known Issues because that later-game scenario was not separately retested.
- Publish the committed source, engine source branch, `v1.1.0` tag, combined release ZIP, and ZIP checksum.
