# Future Skill Development

Learned Skills is a framework with Steal as one module, not a Steal-specific central controller.

A skill definition owns its stable skill ID, manual equipment ID, translation keys, mastery thresholds, rank effects, and description. Generic code owns per-character mastery storage, manual detection, rank calculation, persistence, and the model consumed by UI clients.

A battle skill registers its own `BattleAction`, opts into `BridgedBattleAction`, and contributes it through `GatherBattleActionsEvent` when its definition says it is available. `BattleAction.use` handles initial commitment and `BattleAction.tick` retains multi-frame state through pause, completion, or repeat-turn flow controls. The shared function-160 bridge holds only opted-in custom actions until that lifecycle finishes; skills must not add their own engine patch or retail-action-table branch.

Skill-specific targeting, success formulas, resources, messages, and animation stages belong in that skill's package. For example, a future Quickchange action could use the same mastery services but provide its own availability and state machine. It should not require a `quickchange` branch in Steal or a universal user configuration switch.

The optional menu addon is a presentation adapter only. It reads the generic skill screen model and must never own or duplicate mastery data.

Compatibility rules:

- Keep core source free of `GatherExtensionMenuEntriesEvent`, `ExtensionMenuEntry`, and `ExtensionMenuScreen` linkage.
- Compile and test core against the stock engine jar.
- Put any exact-build menu registration in the addon project.
- Preserve stable registry IDs once released.