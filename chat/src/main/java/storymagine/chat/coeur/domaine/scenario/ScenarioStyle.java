package storymagine.chat.coeur.domaine.scenario;

import java.util.Optional;
import java.util.function.DoubleSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The optional style advice an author writes once in style.txt for every character of a scenario
 * (voice, rhythm, vocabulary — how to write, never what happens). NONE when the file is absent or
 * blank : the prompt is then exactly what it was without the feature.
 * A line may carry a chance marker {20%} anywhere in it : the whole line is then kept (marker
 * removed) in about 20% of the answers and dropped the rest of the time, see drawn().
 */
public record ScenarioStyle(String text) {

    public static final ScenarioStyle NONE = new ScenarioStyle("");

    /** "{20%}" : 1 to 3 digits, spaces tolerated inside the braces. */
    public static final Pattern CHANCE_MARKER = Pattern.compile("\\{\\s*(\\d{1,3})\\s*%\\s*}");

    private static final Pattern DOUBLE_SPACE_BETWEEN_WORDS = Pattern.compile("(?<=\\S)[ \\t]{2,}(?=\\S)");

    public ScenarioStyle {
        text = text == null ? "" : text.strip();
    }

    /** The style for the raw content of style.txt ; blank content means no style. */
    public static ScenarioStyle of(String rawContent) {
        return new ScenarioStyle(rawContent);
    }

    public boolean isPresent() {
        return !text.isEmpty();
    }

    /** This style followed by `other` (its lines come last, so they win when they contradict) ; an empty side is ignored. */
    public ScenarioStyle then(ScenarioStyle other) {
        if (!other.isPresent()) return this;
        return isPresent() ? new ScenarioStyle(text + "\n" + other.text) : other;
    }

    /**
     * The style for one answer : each line with a chance marker is drawn on its own (random gives
     * values in [0, 1)) and either kept without its marker or dropped altogether. Lines without a
     * marker are always kept. Without any marker, the style itself is returned.
     */
    public ScenarioStyle drawn(DoubleSupplier random) {
        if (!CHANCE_MARKER.matcher(text).find()) return this;
        return new ScenarioStyle(text.lines()
            .map(line -> drawLine(line, random))
            .flatMap(Optional::stream)
            .collect(Collectors.joining("\n")));
    }

    private static Optional<String> drawLine(String line, DoubleSupplier random) {
        Matcher marker = CHANCE_MARKER.matcher(line);
        if (!marker.find()) return Optional.of(line);
        int percent = Integer.parseInt(marker.group(1));
        if (random.getAsDouble() * 100 >= percent) return Optional.empty();
        String withoutMarker = CHANCE_MARKER.matcher(line).replaceAll("");
        return Optional.of(DOUBLE_SPACE_BETWEEN_WORDS.matcher(withoutMarker).replaceAll(" ").strip());
    }
}
