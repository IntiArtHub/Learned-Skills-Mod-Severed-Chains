package skill.system.battle;

import legend.game.combat.bent.PlayerBattleEntity;
import legend.game.combat.encounters.MelbuEncounter;
import legend.game.combat.postbattleactions.BossKillPostBattleAction;
import legend.game.combat.postbattleactions.VictoryPostBattleAction;
import legend.game.combat.postbattleactions.PlayFmvPostBattleActionInstance;
import legend.game.combat.postbattleactions.PostBattleActionInstance;
import legend.game.modding.events.battle.BattleEndedEvent;
import skill.system.SkillSystemRuntime;
import skill.system.api.SkillDefinition;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

import static legend.game.Scus94491BpeSegment_8006.battleState_8006e398;
import static legend.game.Scus94491BpeSegment_800b.postBattleAction_800bc974;
import static legend.lodmod.LodMod.HP_STAT;

public final class BattleSkillMastery {
  private static final BattleSkillUseLedger USES = new BattleSkillUseLedger();
  private static final Set<String> QUICKCHANGE_MANUAL_USERS = new HashSet<>();
  private BattleSkillMastery() { }

  public static void clear() { USES.clear(); QUICKCHANGE_MANUAL_USERS.clear(); }
  public static void record(final PlayerBattleEntity player, final SkillDefinition skill) {
    USES.record(SkillSystemRuntime.characterId(player.character), skill.id());
  }

  public static void recordQuickchange(final PlayerBattleEntity player, final boolean usedThroughManual) {
    record(player, SkillSystemRuntime.QUICKCHANGE);
    if(usedThroughManual) QUICKCHANGE_MANUAL_USERS.add(SkillSystemRuntime.characterId(player.character));
  }

  public static void finish(final BattleEndedEvent event) {
    final var ending = postBattleAction_800bc974;
    final boolean victory = isVictory(ending, event.encounter instanceof MelbuEncounter);
    final Set<String> manualUsers = Set.copyOf(QUICKCHANGE_MANUAL_USERS);
    QUICKCHANGE_MANUAL_USERS.clear();
    final Map<String, PlayerBattleEntity> players = new HashMap<>();
    for(int slot = 0; slot < battleState_8006e398.getPlayerCount(); slot++) {
      final var state = battleState_8006e398.playerBents_e40.get(slot);
      if(state != null) players.put(SkillSystemRuntime.characterId(state.innerStruct_00.character), state.innerStruct_00);
    }
    // BattleEndedEvent precedes the engine's post-battle HP floor of 1.
    USES.finish(victory, id -> players.containsKey(id) && players.get(id).stats.getStat(HP_STAT.get()).getCurrent() > 0,
      (id, skillId) -> {
        for(final SkillDefinition skill : SkillSystemRuntime.skills()) {
          if(skill.id().equals(skillId)) SkillSystemRuntime.addSkillMastery(players.get(id).character, skill, 1,
            skill == SkillSystemRuntime.QUICKCHANGE && manualUsers.contains(id));
        }
      });
  }

  static boolean isVictory(final PostBattleActionInstance<?, ?> ending, final boolean melbuEncounter) {
    return ending != null && (ending.action instanceof VictoryPostBattleAction
      || ending.action instanceof BossKillPostBattleAction
      || melbuEncounter && ending instanceof PlayFmvPostBattleActionInstance fmv && fmv.fmv == 16);
  }
}
