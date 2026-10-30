package storymagine.chat.coeur.domaine.session;

import java.util.regex.Pattern;

/**
 * The "(...)" convention : text the player puts between parentheses is their character's inner
 * thought. It stays in the stored/displayed history but is removed before any LLM sees it (see
 * ChatPromptBuilder.transcript()), because a small model cannot reliably ignore what it has read —
 * telling it "your character cannot hear this" still leaks the thought into its reply.
 */
public final class PlayerThoughts {

    private PlayerThoughts() {}

    /** An innermost pair : no parenthesis inside, so nested thoughts are peeled from the inside out. */
    private static final Pattern THOUGHT = Pattern.compile("\\([^()]*\\)");
    private static final Pattern EXTRA_SPACES = Pattern.compile("[ \\t]{2,}");

    /** The text without its "(...)" thoughts, spacing tidied ; blank when the message was only thoughts. */
    public static String strip(String text) {
        String previous;
        String current = text;
        do {
            previous = current;
            current = THOUGHT.matcher(previous).replaceAll("");
        } while (!current.equals(previous));
        return EXTRA_SPACES.matcher(current).replaceAll(" ").strip();
    }
}
