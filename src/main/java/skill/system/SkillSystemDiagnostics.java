package skill.system;

import org.apache.logging.log4j.Logger;

/** Diagnostics enabled with JVM property {@code -Dskill_system.debug=true}. */
public final class SkillSystemDiagnostics {
  public static final String DEBUG_PROPERTY = "skill_system.debug";
  public static final String TEST_MASTERY_PROPERTY = "skill_system.testMastery";
  public static final String FORCE_SUCCESS_PROPERTY = "skill_system.forceStealSuccess";
  public static final String FORCE_GOLD_PROPERTY = "skill_system.forceStealGold";

  private SkillSystemDiagnostics() { }

  public static boolean enabled() {
    return Boolean.getBoolean(DEBUG_PROPERTY);
  }

  public static boolean forceStealSuccess() {
    return enabled() && Boolean.getBoolean(FORCE_SUCCESS_PROPERTY);
  }

  public static Integer forcedStealGold() {
    if(!enabled()) return null;
    final String value = System.getProperty(FORCE_GOLD_PROPERTY);
    if(value == null || value.isBlank()) return null;
    try {
      return Math.max(0, Integer.parseInt(value));
    } catch(final NumberFormatException ignored) {
      return null;
    }
  }

  public static void log(final Logger logger, final String format, final Object... args) {
    if(enabled()) logger.info("[SkillSystem] " + format, args);
  }
}
