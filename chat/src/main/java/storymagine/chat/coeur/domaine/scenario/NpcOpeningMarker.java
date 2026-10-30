package storymagine.chat.coeur.domaine.scenario;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds and removes the "{npc-starts}" / "{npc-starts Name}" command an author writes in a scenario
 * header or in an act, to have an Npc speak first. The command must be ALONE on its line : a mention
 * inside a sentence (e.g. an explanatory note) is never taken for the real thing. Any case. The line
 * is cut out of the text, so the LLM never reads it and Teaser never mistakes it for a story beat.
 * When several commands are present, the first one decides.
 */
public final class NpcOpeningMarker {

    private static final Pattern LINE = Pattern.compile(
        "^[ \\t]*\\{npc-starts(?:[ \\t]+([^}\\n]*))?[ \\t]*}[ \\t]*(?:\\n|$)",
        Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    /** text without any command line, plus the opening the first command asked for (NONE if there was none). */
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
