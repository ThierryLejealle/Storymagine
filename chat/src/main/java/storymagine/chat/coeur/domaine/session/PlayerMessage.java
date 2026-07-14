package storymagine.chat.coeur.domaine.session;

/**
 * What the player typed, parsed for the "DO: " prefix : it hands the narrator's pen to the LLM
 * for this reply only (see ChatPromptBuilder). Everything else is a normal in-character line.
 */
public record PlayerMessage(String text, Mode mode) {

    public enum Mode { DIALOGUE, NARRATOR_HANDOFF }

    private static final String DO_PREFIX = "DO:";

    public static PlayerMessage parse(String raw) {
        String trimmed = raw == null ? "" : raw.strip();
        if (trimmed.regionMatches(true, 0, DO_PREFIX, 0, DO_PREFIX.length())) {
            return new PlayerMessage(trimmed.substring(DO_PREFIX.length()).strip(), Mode.NARRATOR_HANDOFF);
        }
        return new PlayerMessage(trimmed, Mode.DIALOGUE);
    }

    /** The line as it should appear in the prompt / stored history : "DO: " prefix restored for a narrator handoff. */
    public String formattedLine() {
        return mode == Mode.NARRATOR_HANDOFF ? DO_PREFIX + " " + text : text;
    }
}
