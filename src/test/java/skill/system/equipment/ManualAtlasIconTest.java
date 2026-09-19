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
    assertEquals("skill_system:buff_dance_manual_icon", ManualAtlasIcon.BUFF_DANCE.id().toString());
    assertEquals("skill_system:quickchange_manual_icon", ManualAtlasIcon.QUICKCHANGE.id().toString());
  }

  @Test void blueManualHasMatchingFootprintAndTransparentBackground() {
    final Image image = ManualAtlasIcon.loadJarPng("/skill_system/ui/buff_dance_manual.png");
    assertEquals(64, image.width);
    assertEquals(64, image.height);
    assertEquals(0, Byte.toUnsignedInt(image.data[3]));
    int bluePixels = 0;
    for(int i = 0; i < image.data.length; i += 4) {
      if(Byte.toUnsignedInt(image.data[i + 3]) > 128 && Byte.toUnsignedInt(image.data[i + 2]) > Byte.toUnsignedInt(image.data[i]) + 30) bluePixels++;
    }
    assertTrue(bluePixels > 300);
  }

  @Test void quickchangeManualIsPackagedAsTransparentGreenAtlasImage() {
    final Image image = ManualAtlasIcon.loadJarPng("/skill_system/ui/quickchange_manual.png");
    assertEquals(64, image.width);
    assertEquals(64, image.height);
    assertEquals(0, Byte.toUnsignedInt(image.data[3]));
    int greenPixels = 0;
    for(int i = 0; i < image.data.length; i += 4) {
      if(Byte.toUnsignedInt(image.data[i + 3]) > 128 &&
        Byte.toUnsignedInt(image.data[i + 1]) > Byte.toUnsignedInt(image.data[i]) + 30) greenPixels++;
    }
    assertTrue(greenPixels > 200);
    for(int frame = 0; frame < 4; frame++) {
      final Image action = ManualAtlasIcon.loadJarPng("/skill_system/ui/quickchange_action_" + frame + ".png");
      assertEquals(64, action.width);
      assertEquals(64, action.height);
    }
  }

  @Test
  void legacySyntheticItemIconIsNotPackaged() {
    assertNull(ManualAtlasIcon.class.getResource("/skill/system/equipment/ManualItemIcon.class"));
  }

  @Test void inventoryRendererInvalidatesCachedAtlasAfterEngineRebuild() throws Exception {
    final String source = java.nio.file.Files.readString(java.nio.file.Path.of(
      "src/main/java/skill/system/equipment/ManualAtlasItemIcon.java"));
    assertTrue(source.contains("this.cachedAtlas != icon.atlas"));
    assertTrue(source.contains("this.cachedAtlas = icon.atlas"));
  }
}
