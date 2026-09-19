import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Fits an imagegen icon into the existing 64px atlas footprint. */
class ResizeManualIcon {
  public static void main(String[] args) throws Exception {
    var source = ImageIO.read(Path.of(args[0]).toFile());
    var output = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
    var g = output.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    g.drawImage(source, 0, 0, 64, 64, null);
    g.dispose();
    ImageIO.write(output, "png", Path.of(args[1]).toFile());
  }
}
