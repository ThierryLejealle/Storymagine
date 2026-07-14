package storymagine.chat.coeur.domaine.scenario;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts the "[...]" story-beat blocks an author scatters through a scenario or act text: short
 * narration shown to the player — and folded into the persisted history — right when that text
 * becomes current. They are left untouched in the source text so the LLM reads them too, in place,
 * unlike "[NEXT ACT]", which the LLM itself emits and which gets stripped from its replies: this
 * one is author input and is never removed.
 *
 * A block's opening "[" must be the first non-blank character of its line, and its closing "]"
 * must be the last non-blank character of its line — everything in between, including line breaks,
 * is the beat's text (blank-joined into one line on extraction). This still safely ignores an
 * inline "[NEXT ACT]" written mid-sentence, e.g. author text "Quand écrire [NEXT ACT] : ..." : that
 * "[" is preceded by other text on the same line, so it never opens a block. Several blocks can
 * follow each other, one per line-pair-or-more, exactly as before.
 */
public final class Teaser {

    private static final Pattern BLOCK = Pattern.compile("^[ \\t]*\\[([^\\[]*?)][ \\t]*$",
        Pattern.MULTILINE);

    private Teaser() {}

    /** All bracketed blocks found in text, in source order, stripped of their brackets. */
    public static List<String> extractAll(String text) {
        List<String> beats = new ArrayList<>();
        if (text == null) return beats;
        Matcher m = BLOCK.matcher(text);
        while (m.find()) beats.add(joinLines(m.group(1)));
        return beats;
    }

    private static String joinLines(String raw) {
        StringBuilder sb = new StringBuilder();
        for (String line : raw.split("\n")) {
            String stripped = line.strip();
            if (stripped.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(stripped);
        }
        return sb.toString();
    }
}
