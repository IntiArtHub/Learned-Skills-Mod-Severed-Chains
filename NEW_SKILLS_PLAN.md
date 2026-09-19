# New skills implementation plan

## Status

- [x] Buff Dance implemented, built, installed and human-tested in-game.
- [x] Quickchange feasibility and equipment-sync probe completed and human-tested.
- [x] Cancellable one-commit picker, Manual access, rank limits and mastery gain tested in-game.
- [x] The v1.1.0 local candidate includes a staged five-slot Quickchange editor with icons, names, net gear deltas, Y/triangle sorting, cancel and one final confirmation. The editor and sorting passed human testing; final icon size/spacing needs a visual retest.
- [x] Removed development probes, animation viewer, and runtime override switches. Only Bastard Sword remains as extra Forest stock, at 30G.
- [ ] Test Manual consumption and visually accept the final menu and animation before publication approval.

## Durable Buff Dance decisions

- Mastery thresholds: 20/60/99; rank strengths: 15/30/50/75%.
- Party-wide ATK, M ATK, DEF, M DEF and SPEED modifiers last each recipient’s next three turns; recasts refresh and do not stack, preserving the stronger active effect.
- Successful use awards one mastery per character/skill on qualifying victory while alive; equipped manual is consumed at maximum mastery and then grants permanent access.
- Forest vendor sells the light-blue Buff Dance Manual for 20G. Steal remains independent.
- Custom four-frame battle icon, bounded dance animation and blue party effect are implemented; no healing behaviour is coupled to the visual effect.
- Menu Addon lists Buff Dance correctly and supports the current private 1779 test core.

Quickchange uses a green closed-book Manual icon. Rank-scaled 1/2/3/unlimited battle uses and the victory/alive mastery flow are accepted in current testing; Manual consumption still needs testing after the next game progression. Completion plays animation 3, then animation 31 for eight ticks, then restores the prior animation.

## Quickchange feasibility status

- Candidate architecture is mod-local targeted equipment synchronisation around the existing nested battle menu and bridged action lifecycle. No mandatory engine patch was found, but no safe generic refresh exists; never replay `initPlayerBattleEntityStats` during battle.
- The desired animation and exactly one turn require a nested menu with a bridged action tick: cancel returns `REPEAT_TURN`; confirmation runs the animation and resumes with `CONTINUE_SCRIPT`.
- Preserve live modifiers, vitals, turn progress, special equipment-derived external flags, inventory movement, and manual provenance across swaps. Victory mastery must not consume a manual based only on the final accessory state.
- Guarded stock equipment types are supported where live effects can be synchronised safely; unsupported gear remains excluded. See `QUICKCHANGE_FEASIBILITY.md` for earlier source evidence.
- The final twelve-pixel icon spacing and motion need human visual acceptance. The debug actions and 1G shop stock have been removed from source. No publication is authorized. A single rollback is retained at `build/quickchange-probe-rollback/`.
