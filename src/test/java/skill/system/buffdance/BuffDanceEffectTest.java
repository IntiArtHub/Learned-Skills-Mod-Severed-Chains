package skill.system.buffdance;

import legend.game.characters.Stat;
import legend.game.characters.StatCollection;
import legend.game.characters.StatType;
import legend.game.characters.UnaryStat;
import legend.game.characters.UnaryStatType;
import org.junit.jupiter.api.Test;
import org.legendofdragoon.modloader.registries.RegistryId;

import static org.junit.jupiter.api.Assertions.*;

final class BuffDanceEffectTest {
  private static final class TestStats extends StatCollection {
    final TestStat stat = new TestStat(new UnaryStatType(), this);
    @Override @SuppressWarnings("unchecked") public <T extends Stat> T getStat(final StatType<T> type) { return (T)this.stat; }
  }
  private static final class TestStat extends UnaryStat {
    TestStat(final StatType<UnaryStat> type, final StatCollection stats) { super(type, stats); }
    void endTurn() { super.turnFinished(null); }
  }

  @Test void ranksApplyActualPercentagesWithNativeIntegerRounding() {
    final TestStat stat = new TestStats().stat;
    stat.setRaw(100);
    final int[] expected = {115, 130, 150, 175};
    for(int rank = 1; rank <= 4; rank++) {
      BuffDanceEffect.refresh(stat, BuffDanceEffect.strength(rank), 3);
      assertEquals(expected[rank - 1], stat.get());
    }
    stat.removeMod(BuffDanceEffect.MOD_ID);
    stat.setRaw(19);
    BuffDanceEffect.refresh(stat, 5, 3);
    assertEquals(19, stat.get());
  }

  @Test void recipientsExpireIndependentlyAndCasterKeepsThreeFutureTurns() {
    final TestStat caster = new TestStats().stat, ally = new TestStats().stat;
    caster.setRaw(100); ally.setRaw(100);
    BuffDanceEffect.refresh(caster, 10, 4);
    BuffDanceEffect.refresh(ally, 10, 3);
    caster.endTurn(); // Casting turn.
    for(int turn = 0; turn < 3; turn++) {
      assertEquals(110, caster.get());
      caster.endTurn();
    }
    assertEquals(100, caster.get());
    assertEquals(110, ally.get()); // Caster's turns never tick another character.
    for(int turn = 0; turn < 3; turn++) ally.endTurn();
    assertEquals(100, ally.get());
  }

  @Test void recastsRefreshWithoutCompoundingOrWeakeningAndLeaveOtherModsAlone() {
    final TestStat stat = new TestStats().stat;
    stat.setRaw(100);
    final RegistryId other = new RegistryId("test", "other");
    stat.addMod(other, new DanceStatMod(10, 8));
    BuffDanceEffect.refresh(stat, 35, 3);
    stat.endTurn(); stat.endTurn();
    BuffDanceEffect.refresh(stat, 5, 3);
    assertEquals(145, stat.get());
    stat.endTurn(); stat.endTurn();
    assertEquals(145, stat.get());
    stat.endTurn();
    assertEquals(110, stat.get());
    assertTrue(stat.hasMod(other));
    assertFalse(stat.hasMod(BuffDanceEffect.MOD_ID));
  }
}
