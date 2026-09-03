package skill.system.equipment;

import legend.game.types.Renderable58;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ManualAtlasItemIconTest {
  @Test
  void inventoryAnchorMovesOnlyTheManagedManualIconTwoPixelsLeft() {
    final ManualAtlasItemIcon icon = new ManualAtlasItemIcon(ManualAtlasIcon.STEAL, -2, 0);

    assertEquals(-2, icon.anchorX());
    assertEquals(0, icon.anchorY());
    assertEquals(16, ManualAtlasItemIcon.FOOTPRINT);
  }

  @Test
  void managedRenderableConfigurationPreservesCallerFlagsAndAppliesNativeContract() {
    final ManualAtlasItemIcon icon = new ManualAtlasItemIcon(ManualAtlasIcon.STEAL, -2, 0);
    final Renderable58 renderable = new Renderable58();

    icon.configure(renderable, 151, 42, Renderable58.FLAG_DELETE_AFTER_RENDER);

    assertEquals(Renderable58.FLAG_DELETE_AFTER_RENDER | Renderable58.FLAG_NO_ANIMATION, renderable.flags_00);
    assertEquals(149, renderable.x_40);
    assertEquals(42, renderable.y_44);
  }
}