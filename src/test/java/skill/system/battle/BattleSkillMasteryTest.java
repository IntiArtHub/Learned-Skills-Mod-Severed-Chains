package skill.system.battle;

import legend.game.combat.postbattleactions.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class BattleSkillMasteryTest {
  @Test void recognisesStockVictoriesAndRejectsUnknownOrUnrelatedScriptedEndings() {
    assertTrue(BattleSkillMastery.isVictory(new VictoryPostBattleAction().inst(), false));
    assertTrue(BattleSkillMastery.isVictory(new BossKillPostBattleAction().inst(), false));
    assertFalse(BattleSkillMastery.isVictory(null, false));
    final var fmv = new PlayFmvPostBattleAction();
    assertTrue(BattleSkillMastery.isVictory(fmv.inst(16), true));
    assertFalse(BattleSkillMastery.isVictory(fmv.inst(16), false));
    assertFalse(BattleSkillMastery.isVictory(fmv.inst(1), true));
    assertFalse(BattleSkillMastery.isVictory(new GameOverPostBattleAction().inst(), false));
  }
}
