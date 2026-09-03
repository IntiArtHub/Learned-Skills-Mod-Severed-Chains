package skill.system.equipment;

import legend.core.renderer.QueuedModelStandard;
import legend.core.gte.MV;
import legend.game.inventory.Equipment;
import legend.game.textures.Image;
import legend.game.textures.RegisterAtlasTexturesEvent;
import legend.game.textures.TextureAtlasIcon;
import legend.game.types.Renderable58;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.legendofdragoon.modloader.registries.RegistryId;
import skill.system.SkillSystemDiagnostics;
import skill.system.SkillSystemMod;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static legend.core.GameEngine.getTextureAtlas;

/** Shared atlas registration/lookup for managed inventory icons and direct battle icons. */
public final class ManualAtlasIcon {
  public static final ManualAtlasIcon STEAL = new ManualAtlasIcon(
    new RegistryId(SkillSystemMod.MOD_ID, "steal_manual_icon"),
    "/skill_system/ui/steal_manual.png",
    -2,
    0
  );

  private static final List<ManualAtlasIcon> ICONS = List.of(STEAL);
  private static final Logger LOGGER = LogManager.getFormatterLogger(ManualAtlasIcon.class);

  private final RegistryId id;
  private final String resource;
  private final ManualAtlasItemIcon inventoryIcon;
  private boolean lookupLogged;
  private boolean battleDrawLogged;

  private ManualAtlasIcon(final RegistryId id, final String resource, final int inventoryAnchorX, final int inventoryAnchorY) {
    this.id = id;
    this.resource = resource;
    this.inventoryIcon = new ManualAtlasItemIcon(this, inventoryAnchorX, inventoryAnchorY);
  }

  public static void registerAll(final RegisterAtlasTexturesEvent event) {
    ICONS.forEach(icon -> icon.register(event));
  }

  public RegistryId id() {
    return this.id;
  }

  public Renderable58 renderInventory(final Equipment equipment, final int x, final int y, final int flags) {
    return this.inventoryIcon.render(equipment, x, y, flags);
  }

  public Renderable58 renderInventoryManual(final Equipment equipment, final int x, final int y, final int flags) {
    return this.inventoryIcon.renderManual(equipment, x, y, flags);
  }

  public QueuedModelStandard renderBattle(final MV transforms) {
    if(!battleDrawLogged) {
      battleDrawLogged = true;
      SkillSystemDiagnostics.log(LOGGER, "Battle manual icon draw reached id=%s", this.id);
    }
    return icon().render(transforms);
  }

  private void register(final RegisterAtlasTexturesEvent event) {
    final Image image = loadJarPng(this.resource);
    event.add(this.id, image);
    SkillSystemDiagnostics.log(LOGGER, "Registered atlas image id=%s resource=%s dimensions=%dx%d",
      this.id, this.resource, image.width, image.height);
  }

  static Image loadJarPng(final String resource) {
    try(final InputStream stream = ManualAtlasIcon.class.getResourceAsStream(resource)) {
      if(stream == null) throw new IllegalStateException("Missing manual icon resource " + resource);
      final BufferedImage image = ImageIO.read(stream);
      if(image == null) throw new IllegalStateException("Unsupported manual icon resource " + resource);

      final byte[] rgba = new byte[image.getWidth() * image.getHeight() * 4];
      int offset = 0;
      for(int y = 0; y < image.getHeight(); y++) {
        for(int x = 0; x < image.getWidth(); x++) {
          final int argb = image.getRGB(x, y);
          rgba[offset++] = (byte)(argb >>> 16);
          rgba[offset++] = (byte)(argb >>> 8);
          rgba[offset++] = (byte)argb;
          rgba[offset++] = (byte)(argb >>> 24);
        }
      }
      return new Image(rgba, image.getWidth(), image.getHeight());
    } catch(final IOException e) {
      throw new IllegalStateException("Unable to load manual icon resource " + resource, e);
    }
  }

  TextureAtlasIcon icon() {
    final TextureAtlasIcon icon = getTextureAtlas().getIcon(this.id);
    if(icon == null) throw new IllegalStateException("Missing packed atlas icon " + this.id);
    if(!lookupLogged) {
      lookupLogged = true;
      SkillSystemDiagnostics.log(LOGGER, "Resolved atlas icon id=%s rectangle=%d,%d %dx%d",
        this.id, icon.rect.x, icon.rect.y, icon.rect.w, icon.rect.h);
    }
    return icon;
  }
}