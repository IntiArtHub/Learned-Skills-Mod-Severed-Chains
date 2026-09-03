package skill.system.steal;

import java.util.Set;

/**
 * Explicit retail monster-ID classification. No reward/name/HP inference is performed at runtime.
 * IDs were taken from named boss Encounter registrations in the matching game source build.
 */
public final class StealBossRegistry {
  private static final Set<Integer> RETAIL_BOSSES = Set.of(
    261, 262,             // Fruegel encounters
    265, 266,             // Kongol encounters
    267, 268,             // Emperor Doel phases
    269, 270, 277,        // Lloyd representations
    275, 287,             // Feyrbrand / Greham
    283, 284, 285,        // Divine Dragon and targetable parts
    288, 289, 290,        // Shirley trial representations
    293, 294, 279,        // Lenus / Regole
    308, 309, 310, 311, 312, 313, 316, 317, 318, 320, 321, 322,
    325, 326, 327,        // Drake and encounter objects
    329, 332, 333, 334,   // Jiango, Urobolus, Fire Bird and parts
    344, 345,             // Magician Faust representations
    381, 376, 377,        // Dark Doel and weapons
    390, 391, 392, 393, 394, 395 // Melbu/test representations
  );

  private StealBossRegistry() { }
  public static boolean isBoss(final int retailMonsterId) { return RETAIL_BOSSES.contains(retailMonsterId); }
  public static Set<Integer> entries() { return RETAIL_BOSSES; }
}
