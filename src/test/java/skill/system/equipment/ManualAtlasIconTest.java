package skill.system.equipment;

import legend.game.textures.Image;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ManualAtlasIconTest {
  @Test
  void packagedPngDecodesToRgbaForTheModAtlas() {
    final Image image = ManualAtlasIcon.loadJarPng("/skill_system/ui/steal_manual.png");

    assertEquals(64, image.width);
    assertEquals(64, image.height);
    assertEquals(64 * 64 * 4, image.data.length);

    boolean hasVisiblePixel = false;
    boolean hasTransparentPixel = false;
    for(int i = 3; i < image.data.length; i += 4) {
      final int alpha = Byte.toUnsignedInt(image.data[i]);
      hasVisiblePixel |= alpha != 0;
      hasTransparentPixel |= alpha != 255;
    }
    assertTrue(hasVisiblePixel, "icon must contain visible pixels");
    assertTrue(hasTransparentPixel, "icon must preserve transparent background pixels");
  }

  @Test
  void atlasIdIsStableAndNamespaced() {
    assertEquals("skill_system:steal_manual_icon", ManualAtlasIcon.STEAL.id().toString());
  }

  @Test
  void legacySyntheticItemIconIsNotPackaged() {
    assertNull(ManualAtlasIcon.class.getResource("/skill/system/equipment/ManualItemIcon.class"));
  }
}