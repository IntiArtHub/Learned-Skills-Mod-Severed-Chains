import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Deterministic 16px logical pixel-art frames rendered into the 64px atlas footprint. */
class GenerateBattleIcons {
  private static final int S = 4;
  private static final Color OUTLINE = new Color(24, 28, 40, 255);
  private static final Color HAND = new Color(232, 220, 188, 255);
  private static final Color HAND_HI = new Color(255, 246, 220, 255);
  private static final Color NOTE = new Color(150, 225, 255, 255);
  private static final Color NOTE_HI = new Color(225, 250, 255, 255);

  public static void main(final String[] args) throws Exception {
    final Path out = Path.of(args[0]);
    for(int frame = 0; frame < 4; frame++) {
      write(out.resolve("steal_action_" + frame + ".png"), frame, false);
      write(out.resolve("buff_dance_action_" + frame + ".png"), frame, true);
    }
  }

  private static void write(final Path path, final int frame, final boolean note) throws Exception {
    final BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = image.createGraphics();
    g.setColor(new Color(0, 0, 0, 0));
    g.fillRect(0, 0, 64, 64);
    g.scale(S, S);
    if(note) drawNote(g, frame); else drawHand(g, frame);
    g.dispose();
    ImageIO.write(image, "png", path.toFile());
  }

  private static void drawHand(final Graphics2D g, final int frame) {
    final int y = frame == 1 ? 3 : frame == 2 ? 2 : 4;
    g.setColor(OUTLINE);
    g.fillRect(5, y + 4, 7, 8);
    if(frame == 2) {
      g.fillRect(2, 1, 3, 8); g.fillRect(5, 0, 3, 9); g.fillRect(8, 1, 3, 8); g.fillRect(11, 3, 3, 6);
    } else if(frame == 1) {
      g.fillRect(3, 2, 3, 7); g.fillRect(6, 1, 3, 8); g.fillRect(9, 2, 3, 8); g.fillRect(12, 4, 2, 5);
    } else {
      g.fillRect(4, 2, 3, 7); g.fillRect(7, 1, 3, 8); g.fillRect(10, 2, 3, 8); g.fillRect(13, 5, 2, 4);
    }
    g.setColor(HAND);
    g.fillRect(6, y + 5, 5, 6);
    if(frame == 2) { g.fillRect(3, 2, 1, 6); g.fillRect(6, 1, 1, 7); g.fillRect(9, 2, 1, 6); g.fillRect(12, 4, 1, 4); }
    else { g.fillRect(5, 3, 1, 5); g.fillRect(8, 2, 1, 6); g.fillRect(11, 3, 1, 6); }
    g.setColor(HAND_HI); g.fillRect(7, y + 5, 2, 2);
  }

  private static void drawNote(final Graphics2D g, final int frame) {
    final int y = frame == 1 ? 1 : frame == 2 ? 3 : frame == 3 ? 1 : 4;
    g.setColor(OUTLINE);
    g.fillRect(9, y + 1, 3, 9); g.fillRect(6, y + 8, 5, 3); g.fillRect(11, y + 1, 3, 2);
    g.setColor(NOTE); g.fillRect(10, y + 2, 2, 7); g.fillRect(7, y + 8, 3, 2); g.fillRect(12, y + 2, 2, 2);
    g.setColor(NOTE_HI); g.fillRect(10, y + 2, 1, 3);
  }
}
