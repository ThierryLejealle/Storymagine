package storymagine.chat.coeur.domaine.scenario;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds and removes the "{npc-only names}" line an author writes in the first scene (see StartingRoster) :
 * it must be ALONE on its line, and is cut out of the text because the text goes to every character.
 * Any "{npc-only" the pattern does not accept (mid-sentence, no name...) is an error, never silently kept.
 */
public final class NpcOnlyMarker {

    private static final Pattern LINE = Pattern.compile(
        "^[ \\t]*\\{npc-only[ \\t]+([^}\\n]+?)[ \\t]*}[ \\t]*(?:\\n|$)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern ANY = Pattern.compile("\\{\\s*npc-only(?![\\p{L}\\p{N}_-])", Pattern.CASE_INSENSITIVE);

    /** text without any command line, plus the names text of each command found (more than one is an error for the caller). */
    public record Extracted(String text, List<String> namesTexts) {}

    private NpcOnlyMarker() {}

    public static Extracted extract(String text) {
        if (text == null) return new Extracted("", List.of());
        List<String> found = new ArrayList<>();
        Matcher matcher = LINE.matcher(text);
        while (matcher.find()) found.add(matcher.group(1).strip());
        String rest = LINE.matcher(text).replaceAll("");
        Matcher leftover = ANY.matcher(rest);
        if (leftover.find()) {
            String from = rest.substring(leftover.start());
            throw new IllegalArgumentException("Commande {npc-only} invalide : " + from.lines().findFirst().orElse(from)
                + " (attendu, seul sur sa ligne : {npc-only noms})");
        }
        return new Extracted(rest, found);
    }
}
