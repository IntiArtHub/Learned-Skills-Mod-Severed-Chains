package skill.system.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.ToIntFunction;

/** Word-boundary wrapping using the actual rendered-width function supplied by the active font. */
final class TextWrap {
  private TextWrap() { }

  static String wrap(final String text, final int maxWidth, final ToIntFunction<String> measure) {
    Objects.requireNonNull(text, "text");
    Objects.requireNonNull(measure, "measure");
    if(maxWidth <= 0) throw new IllegalArgumentException("maxWidth must be positive");

    final List<String> lines = new ArrayList<>();
    for(final String paragraph : text.split("\\n", -1)) {
      if(paragraph.isBlank()) {
        lines.add("");
        continue;
      }

      String line = "";
      for(final String word : paragraph.trim().split("\\s+")) {
        final String candidate = line.isEmpty() ? word : line + ' ' + word;
        if(measure.applyAsInt(candidate) <= maxWidth) {
          line = candidate;
        } else {
          if(!line.isEmpty()) lines.add(line);
          if(measure.applyAsInt(word) <= maxWidth) {
            line = word;
          } else {
            final List<String> pieces = splitLongWord(word, maxWidth, measure);
            lines.addAll(pieces.subList(0, pieces.size() - 1));
            line = pieces.getLast();
          }
        }
      }
      lines.add(line);
    }
    return String.join("\n", lines);
  }

  private static List<String> splitLongWord(final String word, final int maxWidth, final ToIntFunction<String> measure) {
    final List<String> pieces = new ArrayList<>();
    String piece = "";
    for(int i = 0; i < word.length(); i++) {
      final String candidate = piece + word.charAt(i);
      if(!piece.isEmpty() && measure.applyAsInt(candidate) > maxWidth) {
        pieces.add(piece);
        piece = String.valueOf(word.charAt(i));
      } else {
        piece = candidate;
      }
    }
    pieces.add(piece);
    return pieces;
  }
}
