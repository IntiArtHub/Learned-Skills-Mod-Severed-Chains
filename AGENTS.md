# Learned Skills — Project Instructions

## Project

Learned Skills is a reusable Severed Chains mod framework for character-specific abilities learned through equippable Skill Manuals.

Preserve existing internal `skill_system` registry and save identifiers for compatibility even though the public feature is branded Learned Skills.

## Architecture

The core Learned Skills JAR must remain a normal stock-engine-compatible mod unless a task explicitly requires otherwise.

The optional Menu Addon is separate from the core mod and may depend on an exact-build patched Severed Chains engine. Do not move gameplay functionality into the Menu Addon or patched engine merely for convenience.

Steal is the reference implementation for registered battle actions, targeting, mastery and Skill Manual behaviour.

The mod-local `BridgedBattleAction` / combat-menu function-160 bridge exists because unknown registered actions otherwise map to `ACTION_NOOP`. Preserve native battle-action behaviour and avoid double-ending turns.

## Repository navigation

For core skill-system behaviour, start with:

* `src/main/java/skill/system/SkillSystemRuntime.java`
* `src/main/java/skill/system/SkillSystemMod.java`
* `src/main/java/skill/system/battle/CustomBattleActionBridge.java`
* existing skill action implementations such as `steal/StealBattleAction.java`

Use `PROJECT_STATE.md` for the current Severed Chains build, active engine worktree, test-game location and current development milestone rather than duplicating changing state here.

## Development constraints

Prefer mod-local solutions over engine patches.

When a proposed skill interacts with engine state that is normally initialised only once, investigate the full state lifecycle before implementation. Do not assume changing persistent character data automatically updates the live battle entity.

Keep optional Menu Addon work separate from core gameplay changes unless the task genuinely requires both.

Do not alter, replace or publish existing public release assets, tags, release branches or engine builds unless explicitly instructed.

Do not push, publish, install into the real test game, or modify shared engine branches merely as part of investigation unless explicitly required by the task.

## Investigation workflow

For uncertain or high-risk engine behaviour:

1. determine the relevant stock-engine lifecycle;
2. identify every state representation affected;
3. look for an existing engine refresh/recalculation path;
4. determine whether the feature is safe mod-locally, requires restrictions, or requires engine support;
5. only then implement the chosen design.

Use narrow read-only sub-agent investigations for independent source questions where useful. The primary agent owns architectural conclusions and integration.

## Verification

Prefer focused builds and tests related to changed behaviour before broader checks.

Clearly separate:

* automated verification;
* copied-sandbox or installer verification;
* actual in-game human verification.

Do not claim gameplay behaviour is verified solely because code builds or unit tests pass.
