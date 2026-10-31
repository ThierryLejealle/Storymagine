package storymagine.chat.coeur.domaine.pick;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * One paragraph of a pick library. A line made only of "-" cuts it into pieces (item1, item2... of a
 * "{repeat ...}" block) ; without such a line it is a single piece. A line "- text" is plain text, and
 * "---" (three dashes or more) is the separator between paragraphs, handled by PickParagraphs.
 */
public record PickParagraph(String text) {

    private static final Pattern PIECE_SEPARATOR = Pattern.compile("(?m)^-[ \\t]*$");

    /** The pieces in order, stripped, empty ones dropped. */
    public List<String> pieces() {
        return Arrays.stream(PIECE_SEPARATOR.split(text))
            .map(String::strip)
            .filter(piece -> !piece.isEmpty())
            .toList();
    }

    /** The whole paragraph, the "-" lines removed : what "{item}" stands for. */
    public String withoutSeparators() {
        return String.join("\n", pieces());
    }

    /** The beginning of the paragraph, to point at it in an error message. */
    public String preview() {
        String oneLine = text.replaceAll("\\s+", " ");
        return oneLine.length() <= 40 ? oneLine : oneLine.substring(0, 40) + "...";
    }
}
