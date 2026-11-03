package storymagine.chat.coeur.domaine.scenario;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds and removes the "{npc-story on|off Name tag}" lines an author writes in an act (see
 * StoryDirective). A command must be ALONE on its line ; the tag is its LAST word, so a character
 * name may have several words. The lines are cut out of the text because it goes to every character.
 * Any leftover "{npc-story" the pattern does not accept is an error, never silently kept in the text.
 */
public final class StoryDirectiveMarker {

    private static final Pattern LINE = Pattern.compile(
        "^[ \\t]*\\{npc-story[ \\t]+(on|off)[ \\t]+([^}\\n]+?)[ \\t]+([^\\s}]+)[ \\t]*}[ \\t]*(?:\\n|$)",
        Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern ANY = Pattern.compile("\\{\\s*npc-story(?![\\p{L}\\p{N}_-])", Pattern.CASE_INSENSITIVE);

    /** text without any command line, plus the directives in the order written. */
    public record Extracted(String text, List<StoryDirective> directives) {}

    private StoryDirectiveMarker() {}

    public static Extracted extract(String text) {
        if (text == null) return new Extracted("", List.of());
        List<StoryDirective> directives = new ArrayList<>();
        Matcher m = LINE.matcher(text);
        while (m.find()) {
            directives.add(new StoryDirective(m.group(1).equalsIgnoreCase("on"), m.group(2).strip(), m.group(3)));
        }
        String rest = LINE.matcher(text).replaceAll("");
        Matcher leftover = ANY.matcher(rest);
        if (leftover.find()) {
            String from = rest.substring(leftover.start());
            throw new IllegalArgumentException("Commande npc-story invalide : " + from.lines().findFirst().orElse(from)
                + " (attendu, seul sur sa ligne : {npc-story on|off nom tag})");
        }
        return new Extracted(rest, directives);
    }
}
