package skill.system.menu;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TextWrapTest {
  @Test
  void wrapsDescriptionAtWordBoundariesWithinMeasuredWidth() {
    final String wrapped = TextWrap.wrap("Attempts to steal an enemy's held item or gold.", 20, String::length);
    assertEquals("Attempts to steal an\nenemy's held item or\ngold.", wrapped);
    assertTrue(Arrays.stream(wrapped.split("\\n")).allMatch(line -> line.length() <= 20));
  }

  @Test
  void preservesExplicitEffectLinesAndWrapsEachIndependently() {
    final String wrapped = TextWrap.wrap("Success Rate ×3\nBoss Steal enabled", 12, String::length);
    assertEquals("Success Rate\n×3\nBoss Steal\nenabled", wrapped);
    assertTrue(Arrays.stream(wrapped.split("\\n")).allMatch(line -> line.length() <= 12));
  }

  @Test
  void splitsAnExceptionalOverwideTokenWithoutOverflowing() {
    final String wrapped = TextWrap.wrap("extraordinary", 5, String::length);
    assertEquals("extra\nordin\nary", wrapped);
  }

  @Test
  void skillsDetailRegionsHaveExplicitNativePanelBounds() {
    assertEquals(152, SkillsScreen.DETAIL_TEXT_WIDTH);
    assertEquals(0.8f, SkillsScreen.DETAIL_BODY_SCALE);
  }
}
