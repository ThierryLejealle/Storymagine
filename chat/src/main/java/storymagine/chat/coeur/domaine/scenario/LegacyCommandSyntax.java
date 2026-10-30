package storymagine.chat.coeur.domaine.scenario;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Refuses the old bracket spelling of the author commands ("[PICK: x]", "[PICK-UNIQUE: x]",
 * "[NPC STARTS]") : left alone they would silently become story beats shown to the player. "[NEXT ACT]" is not a command and stays valid.
 */
public final class LegacyCommandSyntax {

    private static final Pattern LEGACY = Pattern.compile(
        "\\[\\s*(?:PICK(?:-UNIQUE)?\\s*:|NPC[ _-]+STARTS?\\b)[^\\]]*]", Pattern.CASE_INSENSITIVE);

    private LegacyCommandSyntax() {}

    /** IllegalArgumentException naming the first old-style command found in text ; nothing when there is none. */
    public static void requireNone(String text) {
        if (text == null) return;
        Matcher m = LEGACY.matcher(text);
        if (m.find()) {
            throw new IllegalArgumentException("Ancienne syntaxe de commande : " + m.group()
                + " -> les commandes s'ecrivent maintenant entre accolades ({pick random lieu}, {npc-starts}). "
                + "Voir le guide de redaction des scenarios.");
        }
    }
}
