package storymagine.chat.coeur.domaine.scenario;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The "{20%}" chance markers in the text of a scenario file (scenario.txt and its continuations): a line
 * that carries one is kept (marker removed) with that chance and dropped altogether otherwise. The draw is
 * made once, when the game starts, like a "{pick}" (see PickResolver) : the scene then holds the same text
 * for the whole game, for every character, and a retry or a "go back" does not change it. The same marker in
 * style.txt is drawn at every answer instead (see ScenarioStyle.drawn).
 */
public final class ChanceLines {

    private static final Pattern DOUBLE_SPACE_BETWEEN_WORDS = Pattern.compile("(?<=\\S)[ \\t]{2,}(?=\\S)");

    private ChanceLines() {}

    public static boolean appearsIn(String text) {
        return text != null && ScenarioStyle.CHANCE_MARKER.matcher(text).find();
    }

    /**
     * The text with each marked line drawn : nextInt(100) gives a number from 0 to 99, the line is kept
     * when it is under the percent. Lines without a marker, and the line breaks around them, are untouched.
     */
    public static String draw(String text, IntUnaryOperator nextInt) {
        if (!appearsIn(text)) return text;
        List<String> kept = new ArrayList<>();
        for (String line : text.split("\n", -1)) {
            Matcher marker = ScenarioStyle.CHANCE_MARKER.matcher(line);
            if (!marker.find()) {
                kept.add(line);
                continue;
            }
            int percent = Integer.parseInt(marker.group(1));
            if (nextInt.applyAsInt(100) >= percent) continue;
            String withoutMarker = DOUBLE_SPACE_BETWEEN_WORDS.matcher(ScenarioStyle.CHANCE_MARKER.matcher(line).replaceAll("")).replaceAll(" ");
            boolean markerFirst = line.stripLeading().startsWith(marker.group());
            kept.add(markerFirst ? withoutMarker.strip() : withoutMarker.stripTrailing());
        }
        return String.join("\n", kept);
    }
}
