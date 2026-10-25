package storymagine.chat.coeur.domaine.scenario;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds and removes the "[NPC STARTS]" / "[NPC STARTS: Name]" marker an author writes in a scenario
 * header or in an act, to have an Npc speak first. The marker must be ALONE on its line : a mention
 * inside a sentence (e.g. an explanatory note) is never taken for the real thing. Tolerant like
 * NextActMarker (any case, '_' or '-' between the words, "STARTS" or "START"). The line is cut out
 * of the text, so the LLM never reads it and Teaser never mistakes it for a story beat. When
 * several markers are present, the first one decides.
 */
public final class NpcOpeningMarker {

    private static final Pattern LINE = Pattern.compile(
        "^[ \\t]*\\[NPC[ _-]+STARTS?(?:[ \\t]*:[ \\t]*([^\\]\\n]*))?][ \\t]*(?:\\n|$)",
        Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    /** text without any marker line, plus the opening the first marker asked for (NONE if there was none). */
    public record Extracted(String text, NpcOpening opening) {}

    private NpcOpeningMarker() {}

    public static Extracted extract(String text) {
        if (text == null) return new Extracted("", NpcOpening.NONE);
        Matcher m = LINE.matcher(text);
        NpcOpening opening = NpcOpening.NONE;
        if (m.find()) opening = NpcOpening.named(m.group(1));
        return new Extracted(LINE.matcher(text).replaceAll(""), opening);
    }
}
