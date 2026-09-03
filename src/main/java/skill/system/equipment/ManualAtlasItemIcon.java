package skill.system.equipment;

import legend.core.renderer.QueuedModelStandard;
import legend.core.gpu.Bpp;
import legend.core.renderer.Obj;
import legend.core.renderer.QuadBuilder;
import legend.core.renderer.Texture;
import legend.game.inventory.Equipment;
import legend.game.textures.TextureAtlasIcon;
import legend.game.types.Renderable58;
import legend.game.types.RenderableMetrics14;
import legend.game.types.UiPart;
import legend.game.types.UiType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import skill.system.SkillSystemDiagnostics;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static legend.game.Menus.addToManagedRenderables;
import static legend.game.Menus.allocateManualRenderable;

/** A mod-atlas icon that honours the managed lifetime contract of {@code ItemIcon}. */
public final class ManualAtlasItemIcon {
  static final int FOOTPRINT = 16;
  private static final Logger LOGGER = LogManager.getFormatterLogger(ManualAtlasItemIcon.class);

  private final ManualAtlasIcon atlasIcon;
  private final int anchorX;
  private final int anchorY;
  private final Set<String> loggedContexts = ConcurrentHashMap.newKeySet();
  private UiType uiType;

  ManualAtlasItemIcon(final ManualAtlasIcon atlasIcon, final int anchorX, final int anchorY) {
    this.atlasIcon = atlasIcon;
    this.anchorX = anchorX;
    this.anchorY = anchorY;
  }

  public Renderable58 render(final Equipment equipment, final int x, final int y, final int flags) {
    final Renderable58 renderable = this.renderManual(equipment, x, y, flags);
    addToManagedRenderables(renderable);
    return renderable;
  }

  public Renderable58 renderManual(final Equipment equipment, final int x, final int y, final int flags) {
    final TextureAtlasIcon resolved = this.atlasIcon.icon();
    final Renderable58 renderable = allocateManualRenderable(this.uiType(resolved), null);
    this.configure(renderable, x, y, flags);
    this.logInvocation(equipment, x, y, flags, resolved);
    return renderable;
  }

  int anchorX() {
    return this.anchorX;
  }

  int anchorY() {
    return this.anchorY;
  }

  void configure(final Renderable58 renderable, final int x, final int y, final int flags) {
    renderable.flags_00 |= flags | Renderable58.FLAG_NO_ANIMATION;
    renderable.x_40 = x + this.anchorX;
    renderable.y_44 = y + this.anchorY;
  }

  private UiType uiType(final TextureAtlasIcon icon) {
    if(this.uiType == null) {
      final Texture texture = icon.atlas.texture;
      final RenderableMetrics14 metrics = new AtlasMetrics(texture);
      final UiType type = new UiType(new UiPart[] {new UiPart(new RenderableMetrics14[] {metrics}, 0)});
      final Obj obj = new QuadBuilder("Skill Manual atlas-backed ItemIcon " + this.atlasIcon.id())
        .bpp(Bpp.BITS_24)
        .posSize(1.0f, 1.0f)
        .uv(icon.rect.x / (float)texture.width, icon.rect.y / (float)texture.height)
        .uvSize(icon.rect.w / (float)texture.width, icon.rect.h / (float)texture.height)
        .build();
      obj.persistent = true;
      type.obj = obj;
      this.uiType = type;
      SkillSystemDiagnostics.log(LOGGER,
        "Built managed atlas ItemIcon id=%s rectangle=%d,%d %dx%d footprint=%dx%d anchor=%d,%d path=managed-renderable",
        this.atlasIcon.id(), icon.rect.x, icon.rect.y, icon.rect.w, icon.rect.h,
        FOOTPRINT, FOOTPRINT, this.anchorX, this.anchorY);
    }
    return this.uiType;
  }

  private void logInvocation(final Equipment equipment, final int x, final int y, final int flags,
                             final TextureAtlasIcon icon) {
    if(!SkillSystemDiagnostics.enabled()) return;

    final String context = StackWalker.getInstance().walk(frames -> frames
      .filter(frame -> !frame.getClassName().startsWith("skill.system.equipment."))
      .limit(8)
      .map(frame -> frame.getClassName() + '#' + frame.getMethodName())
      .collect(Collectors.joining(" <- ")));

    if(this.loggedContexts.add(context + '|' + flags)) {
      SkillSystemDiagnostics.log(LOGGER,
        "Manual inventory icon invocation context=%s equipmentClass=%s registryId=%s x=%d y=%d flags=0x%x " +
          "atlasLookup=resolved atlasRect=%d,%d %dx%d drawPath=managed-renderable",
        context, equipment.getClass().getName(), equipment.getRegistryId(), x, y, flags,
        icon.rect.x, icon.rect.y, icon.rect.w, icon.rect.h);
    }
  }

  private static final class AtlasMetrics extends RenderableMetrics14 {
    private final Texture texture;

    private AtlasMetrics(final Texture texture) {
      super(0.0f, 0.0f, 0, 0, 0, 0, FOOTPRINT, FOOTPRINT, 1.0f, 1.0f);
      this.texture = texture;
    }

    @Override
    public void useTexture(final QueuedModelStandard model) {
      model.texture(this.texture).useTextureAlpha();
    }
  }
}