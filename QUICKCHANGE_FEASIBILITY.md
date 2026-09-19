# Quickchange feasibility — 1779 source investigation

> Historical investigation report. Superseded by the implemented Quickchange in the local v1.1.0 release candidate; retain this file as source evidence. The candidate is unpublished.

Date: 2026-09-16. At the time, this was investigation only; later work implemented and tested the feature locally.

## Judgement

**A mod-local Quickchange with an explicit equipment compatibility policy is feasible in principle (option 2). No mandatory engine change was found.** Full stock-equipment support (option 1) remains plausible: the source exposes the required state, and stock weapons do not individually select battle models. It has not yet been demonstrated safe at runtime. The evidence does **not** establish that stock weapons or all accessories must be permanently excluded.

An unrestricted adapter for arbitrary mod equipment cannot safely infer hook side effects or distinguish equipment contributions from unrelated script changes. A general engine-supported equipment transaction/contribution lifecycle would be the appropriate option 3 for that broader guarantee. Merely adding a public refresh method would not solve those ownership problems.

Recommend selecting a targeted mod-local synchroniser, initially limited to explicitly audited equipment/effects, and proving it with the small experiment below before implementing the feature. Restrictions are a proposed first support boundary, not a user-approved final design.

## Evidence baseline

- Read project `AGENTS.md`, `PROJECT_STATE.md`, current `NEW_SKILLS_PLAN.md`, and historical `NEW_SKILLS_HANDOFF.md`. The latest request supersedes their earlier Quickchange deferral only for feasibility work.
- Engine source: `../_shared/worktrees/Severed-Chains-learned-skills-b2a49e4`, HEAD `85814975e8e328723ed1ba4f4ab4531c94896632`, based on stock `b2a49e4df549b1941b1c5af45d0d150f27c2dc4e` / 1779.
- The HEAD-to-stock file diff contains the optional menu extension, build/version/language/test files; it does not alter the combat/equipment files cited here.
- In this report, engine Java paths are relative to that engine's `src/main/java/`; mod paths are relative to this project. Script paths are relative to the engine root.
- Buff Dance remains complete and human-tested. Only its shared stat-modifier and action lifecycle were inspected for preservation/reuse. Existing uncommitted implementation work was left intact.

## State that must stay consistent

There are four relevant representations: inventory ownership, `CharacterData2c` equipment and persistent stat modifiers, `PlayerBattleEntity` equipment and live stats, and external battle/script state.

`Battle.initPlayerBattleEntityStats()` is called during player allocation (`legend/game/combat/Battle.java:2114-2168`). Its equipment snapshot is at `8377-8460`. The complete stock field mapping is:

| Equipment input | Live destination |
|---|---|
| Equipment references by slot | `equipment_11e` |
| `flags_00` | `specialEffectFlag_14` |
| `attackElement_04` | `equipmentAttackElements_1c` |
| `elementalResistance_06`, `elementalImmunity_07` | `equipmentElementalResistance_20`, `equipmentElementalImmunity_22` |
| `statusResist_08` | `equipmentStatusResist_24` |
| `attack_10` | raw ATTACK **and** `equipmentAttack1_28` |
| `magicAttack_11`, `defence_12`, `magicDefence_13` | raw MAGIC_ATTACK, DEFENSE, MAGIC_DEFENSE |
| `attackHit_14`, `magicHit_15` | raw ATTACK_HIT, MAGIC_HIT |
| `attackAvoid_16`, `magicAvoid_17` | raw ATTACK_AVOID, MAGIC_AVOID |
| `getGuardHealBonus()` | raw GUARD_HEAL |
| `onHitStatusChance_18` | `onHitStatusChance_44` |
| `onHitStatus_1b` | `equipmentOnHitStatus_4a` **and** `_142` |
| physical/magical immunity and resistance booleans | `physicalImmunity_110`, `magicalImmunity_112`, `physicalResistance_114`, `magicalResistance_116` |
| `spMultiplier` | `spMultiplier_128` |
| physical/magical SP/MP per-hit bonuses | `spPerPhysicalHit_12a`, `mpPerPhysicalHit_12c`, `spPerMagicalHit_12e`, `mpPerMagicalHit_130` **and their four `original*` baselines** |
| `escapeBonus` | `escapeBonus_132` |
| HP/MP/SP regeneration | `hpRegen_134`, `mpRegen_136`, `spRegen_138` |
| `revive` | `revive_13a` |
| HP/MP multipliers | `hpMulti_13c`, `mpMulti_13e` **and HP/MP maximum stat modifiers** |
| `speed_0f` | permanent contributing SPEED modifier keyed by equipment registry ID |

The HP/MP/SPEED modifiers originate in `Equipment.onEquip()` / `onUnequip()` (`legend/game/inventory/Equipment.java:185-213`), then reach the live entity through `player.stats.set(player.character.stats)` (`Battle.java:8393`). That copy includes current HP/MP/SP and stat modifiers; `legend/game/characters/StatCollection.java:28-32` and `Stat.java:20-25` copy objects/modifiers rather than linking them.

These fields feed damage, hit/avoid, status, SP, regeneration and escape paths. Representative consumers: `PlayerBattleEntity.java:172-249,387-401`; `Battle.java:8471-8497`; `patches/scripts/player_combat_script.txt:6518-6602,6654-6663`. Revival is a chance read at death, not evidence of a spent charge in this path.

There is also state outside the entity: Wargod Calling, Ultimate Wargod and Destroyer Mace `applyEffect()` set bits `0x2`, `0x6`, `0x1` in `battleState.additionExtra_474[slot].flag_00` (their respective classes under `legend/lodmod/equipment`, line 18). The player script clears and caches those at initialization (`player_combat_script.txt:6029-6033,6761-6775`). Replacing a slot map does not remove these bits. Only owned bits may be changed; preserve unrelated fields and automatic-addition configuration (`legend/game/combat/types/battlestate/AdditionExtra04.java:14-56`).

## Normal equipment changes are not battle transactions

- `CharacterData2c.canEquip()` validates template/equipment/type permissions (`legend/game/characters/CharacterData2c.java:122-138`). `equip()` itself does not perform that validation.
- `CharacterData2c.equip()` calls old `onUnequip`, replaces/removes the slot, then calls new `onEquip` (`144-161`). It does not transfer inventory or update a battle entity.
- The normal screen performs equip, removes the incoming inventory item, returns the previous equipment, clamps HP/MP through zero-value updates and redraws (`legend/game/inventory/screens/EquipmentScreen.java:252-266,401-416`). Its browsing/preview path must not be transplanted into combat as an immediate-mutation picker.
- Inventory helpers post cancellable events; giving can also fail or be modified by event listeners (`legend/game/SItem.java:543-585`). Do not assume a multi-call swap is atomic because the normal screen uses it. A Quickchange transaction must validate ownership/capacity and handle failed removal/return without duplicating or losing equipment. Arbitrary listener side effects are outside a simple rollback guarantee.
- Battle exit copies live HP/MP/SP and persistent status back to the character, **not equipment** (`Battle.java:8344-8360`). Persistent equipment must therefore be updated on successful commitment; a battle-only swap would not persist correctly.

## No safe generic refresh exists in this source

**Never replay `initPlayerBattleEntityStats()`.** It replaces live stat objects/vitals/modifiers from stale persistent data, overwrites status/addition inputs, appends spell collections, and adds/ORs equipment aggregates without clearing them. Removed effects survive and additive values grow on repeated calls.

`PlayerBattleEntity.recalculateSpeedAndPerHitStats()` (`289-313`) only rebuilds the four per-hit totals from `original*` plus temporary bonuses; the base implementation is empty (`BattleEntity27c.java:437-440`). It neither reads equipment nor rebuilds HP/MP, resistances or base stats. It is useful **after** correcting the four equipment baselines.

The turn scheduler reads current SPEED directly (`legend/game/combat/types/BattleStateEf4.java:694-698`). Preserve `turnValue_4c`; do not restart turn-order initialization.

Use additive deltas for audited raw equipment stat contributions, preserving existing live stat objects and unrelated modifiers. Transfer only the known equipment-owned HP/MP/SPEED modifiers. Rebuild equipment-only sets/flags from the final loadout, not by blindly removing an old bit that another equipped item also supplies. Preserve temporary immunity/per-hit fields, durations, status, addition, spell lists, action/script state and turn progress. Direct script writes to the same legacy fields lack ownership metadata (`Battle.java:4205-4232`, `BattleEntity27c.setStat()`); broad third-party compatibility needs a stronger contract.

For a maximum-HP/MP change, proposed policy is absolute current value preserved, clamped down to the new maximum; increasing maximum grants no free healing. `FractionalStat.getCurrent()` itself clamps on read (`legend/game/characters/FractionalStat.java:29-36`), so snapshot live values before any preview or change. Re-equipping after a real downward clamp must not restore lost HP/MP. This policy still needs agreement and a runtime check.

## Weapons and animation

The stock model/TIM/attack-animation selection inspected is character-template, form and addition driven (`Battle.java:2889-2910,2988-3002,3275-3282`; `legend/game/characters/CharacterTemplate.java:66-100`). The weapon model part is a template property (`PlayerBattleEntity.java:343-345`). No stock per-equipment model asset switch was found. **Ordinary stock weapons are not inherently excluded by model loading.** This does not promise new per-weapon visuals or compatibility with custom templates that derive assets from equipment.

Form changes have an asynchronous deallocate/reload/rebind sequence (`Battle.java:4158-4191`). It is evidence of an asset-loading mechanism, not an equipment-refresh API; ordinary stock weapon swaps should not invoke it. Custom gear-specific assets would need equivalent lifecycle handling and compatible model-part/animation layouts.

Weapon `prepareAttack()` / `attack()` are dispatched through the current live `equipment_11e` weapon (`Battle.java:1261-1290`). Detonate Arrow is the special callback case: it assigns Detonate Rock and loads its DEFF when preparing an attack (`legend/lodmod/equipment/DetonateArrowEquipment.java:23-33`). Destroyer Mace also needs the external flag update described above. Those cases merit explicit adapters/tests, not a generic call to all equipment hooks or a full character reload.

## Menu, commitment and turn cost

`ChangeAdditionBattleAction.java:7-19` and `AdditionListMenu.java:84-102` demonstrate a battle-local nested list and updating persistent/live state together. They are inspiration, not a complete Quickchange action: Change Addition costs no turn, mutates on selection, and only reloads addition attack animations.

For the proposed one-swap/one-turn design:

1. Capture the actor, permitted candidates and current ownership; show a picker with no equipment mutation during browsing.
2. Cancel returns to command selection with no turn, usage or mastery charge.
3. Confirm revalidates the candidate and commits inventory, persistent equipment and live state together. A same-equipment/no-op selection is not a successful use.
4. After a successful commit, refresh available actions, play the small completion animation, record the skill use, and let the normal bridged action completion consume exactly one turn.

Use the existing `BridgedBattleAction` lifecycle for that last distinction. `Battle.java:4071-4111` pauses while a list exists, ticks the active action afterwards, returns to selection for `REPEAT_TURN`, and continues the player script for `CONTINUE_SCRIPT`. `BattleHud.java:1670-1691` discards an action returning use-time `PAUSE_SCRIPT`, so simply copying Change Addition's return value will not retain the action for a post-menu animation. A candidate design opens the list while retaining the bridged action (`CONTINUE_SCRIPT` intercepted by the existing function-160 bridge); the callback records cancel/commit and the subsequent tick resolves it. This integration is source-supported, not runtime-tested here.

## Skill Manuals require an explicit policy

Availability reads the current persistent accessory (`src/main/java/skill/system/SkillSystemRuntime.java:55-62`). The action list is gathered by `SkillSystemMod.java:88-103`; turn start requests a refresh (`Battle.java:2354-2366`), consumed by function 160 (`4092-4095`). Refresh after a committed swap and revalidate permission when launching the action; do not let an old menu list grant removed-manual access.

`BattleSkillMastery.java:23-42` records character/skill usage, not which manual granted it. `SkillSystemRuntime.java:72-78` commits mastery and consumes only a matching **currently equipped** manual. Moving that manual to inventory before victory therefore lets it escape consumption. Disallowing new manual equips while allowing removals does **not** solve this. Stock equipment entries are registry references rather than uniquely identified physical copies, so retaining an object reference alone is not sufficient provenance if identical manuals can move between owners.

Smallest first policy: lock transitions involving any Skill Manual in either direction; ordinary equipment in other slots can still change. That includes replacing an equipped Quickchange Manual, so unmastered Quickchange initially cannot free its own accessory slot. This is a real user-facing restriction, not an assumed final decision.

If manual removal is required, add a battle-local ownership/reservation ledger with defined inventory/re-equip/other-character rules, and consume the owed copy exactly once at qualifying mastery. Apply it to all affected skills, including Steal and Buff Dance, without revisiting their approved gameplay. Preserve the existing once-per-battle/victory/alive gate. Manual equips/removals should remain unsupported until that policy is selected and tested.

## Smallest design options

| Option | Scope | Assessment |
|---|---|---|
| A: targeted mod-local synchroniser, restricted first scope | Explicitly audited stock equipment; initially ordinary numeric gear, including ordinary weapons; manuals locked; defer special callbacks and unvalidated effects | Recommended first architecture. Smallest state surface with a credible preservation strategy. Eligibility must check both outgoing and incoming equipment, not just slot or incoming class. |
| B: full stock equipment adapter | Extend A across every mapped effect, special flag cache, Detonate Arrow, and chosen manual provenance policy | Plausible without engine changes; greater implementation/QA burden. No intrinsic stock weapon asset blocker found. Not yet safe to promise. |
| C: engine equipment lifecycle/transaction API | Supported refresh with effect ownership/removal hooks, asset invalidation where required, and inventory transaction semantics | Appropriate for general mod interoperability; not currently necessary for A. Larger engine scope and contrary to stock-core preference unless justified later. |

An exact allowed-item/effect list is intentionally not declared from class/slot names alone. An ordinary `Equipment` can still carry immunity, regeneration or maximum-vital effects. Unsupported outgoing equipment must be blocked as well; removing it is also a state change.

## Proposed isolated experiment — not performed

Use a disposable copied 1779 sandbox and a disposable diagnostic mod/harness. No picker, new manual, shop entry, mastery progression, rank system, release build or changes to the current test installation.

First discriminating probe: at a normal player command boundary, swap one preselected plain stock weapon/armour pair and back, then one stock SPEED pair, through a narrow synchroniser. Capture inventory counts/slots, persistent equipment, every mapped live value, HP/MP/SP, status, existing modifier identities/durations, per-hit baselines, and turn progress before/after. Include one existing temporary buff, repeated A→B→A, one cancelled attempt and one failed preflight. Compare equipment-owned results against a fresh-entry reference for the final loadout while separately asserting live-state preservation; do not use a fresh entity as a replacement for the active one.

Acceptance: no drift/duplication/loss, persistent and live loadouts agree, only expected equipment contributions differ, the modifier remains and expires normally, and the next attack uses the expected stats. Human observation covers normal animation and continued battle execution. If this fails, inspect the specific state owner before widening scope or proposing engine work.

Only after that passes, if selecting B, extend the same harness to maximum-HP/MP decrease/increase, overlapping resistance sources, per-hit temporary bonuses, special equipment flag removal/reapply and Detonate Arrow→normal weapon. These are compatibility expansion gates, not part of the minimal first probe. Manual provenance and exactly-once turn completion need separate tests when their design is selected.

## Remaining decisions and limits

- Select restricted first scope versus full-stock target, and whether manual removal is required from the outset.
- Agree maximum-vital policy and manual ownership/consumption semantics. Historical rank-scaled 1/2/3/unlimited uses and light-green manual remain provisional.
- Runtime proof is outstanding: battle continuation, next attacks, form transitions, special scripted encounters, hook/event failures and third-party interactions were not exercised.
- Three Luna workers performed independent source investigations; the primary agent reviewed the state-copy, callback, script-cache and action-flow evidence and owns this judgement.
- This report predates the implementation. The editor and sorting have since received human testing; manual consumption remains pending, and final spacing and animation cleanup still need a visual retest.
