package storymagine.chat.coeur.domaine.scenario;

/**
 * The optional style advice an author writes once in style.txt for every character of a scenario
 * (voice, rhythm, vocabulary — how to write, never what happens). NONE when the file is absent or
 * blank : the prompt is then exactly what it was without the feature.
 */
public record ScenarioStyle(String text) {

    public static final ScenarioStyle NONE = new ScenarioStyle("");

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
}
