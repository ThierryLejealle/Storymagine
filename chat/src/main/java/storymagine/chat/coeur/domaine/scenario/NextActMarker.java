package storymagine.chat.coeur.domaine.scenario;

import java.util.regex.Pattern;

/**
 * Detects and strips the "[NEXT ACT]" marker the LLM emits when it decides the current act is
 * over — tolerant of an author's or a small model's spelling slips (underscore/hyphen instead of
 * a space between the words, extra spaces, any case), since a missed marker silently stalls the
 * story instead of failing loudly. Same pattern used both to read a scenario act's own condition
 * text (ChatPromptBuilder) and to catch the marker in the model's reply (RoleplayNarrator).
 */
public final class NextActMarker {

    public static final Pattern PATTERN = Pattern.compile("\\s*\\[NEXT[ _-]+ACT\\]\\s*", Pattern.CASE_INSENSITIVE);

    private NextActMarker() {}

    public static boolean isPresentIn(String text) {
        return PATTERN.matcher(text).find();
    }
}
