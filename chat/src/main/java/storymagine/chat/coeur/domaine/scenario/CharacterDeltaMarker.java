package storymagine.chat.coeur.domaine.scenario;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds and removes the "{npc-delta public Name : text}" / "{npc-delta secret Name : text}" lines an
 * author writes in an act (see CharacterDelta). A command must be ALONE on its line and complete
 * (visibility, name, text). The lines are cut out of the text because the text of an act goes to
 * every character: a secret left in it would reach all of them. Any leftover "{npc-delta" the
 * pattern does not accept (missing visibility, in the middle of a sentence...) is an error, never
 * silently kept in the text.
 */
public final class CharacterDeltaMarker {

    private static final Pattern LINE = Pattern.compile(
        "^[ \\t]*\\{npc-delta[ \\t]+(public|secret)[ \\t]+([^:}\\n]+?)[ \\t]*:[ \\t]*([^}\\n]+?)[ \\t]*}[ \\t]*(?:\\n|$)",
        Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern ANY = Pattern.compile("\\{\\s*npc-delta", Pattern.CASE_INSENSITIVE);

    /** text without any command line, plus the deltas in the order written. */
    public record Extracted(String text, List<CharacterDelta> deltas) {}

    private CharacterDeltaMarker() {}

    public static Extracted extract(String text) {
        if (text == null) return new Extracted("", List.of());
        List<CharacterDelta> deltas = new ArrayList<>();
        Matcher m = LINE.matcher(text);
        while (m.find()) {
            deltas.add(new CharacterDelta(
                CharacterDelta.Visibility.valueOf(m.group(1).toUpperCase()), m.group(2).strip(), m.group(3).strip()));
        }
        String rest = LINE.matcher(text).replaceAll("");
        Matcher leftover = ANY.matcher(rest);
        if (leftover.find()) {
            String from = rest.substring(leftover.start());
            throw new IllegalArgumentException("Commande npc-delta invalide : " + from.lines().findFirst().orElse(from)
                + " (attendu, seul sur sa ligne : {npc-delta public|secret nom : texte})");
        }
        return new Extracted(rest, deltas);
    }
}
