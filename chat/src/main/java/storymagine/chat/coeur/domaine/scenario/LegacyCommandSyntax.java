package storymagine.chat.coeur.domaine.scenario;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Refuses the old spellings of the author commands : the bracket form ("[PICK: x]", "[PICK-UNIQUE: x]",
 * "[NPC STARTS]"), which left alone would silently become story beats shown to the player, and the old
 * name "{delta-pnj ...}" (now "{npc-delta ...}"). "[NEXT ACT]" is not a command and stays valid.
 */
public final class LegacyCommandSyntax {

    private static final Pattern LEGACY = Pattern.compile(
        "\\[\\s*(?:PICK(?:-UNIQUE)?\\s*:|NPC[ _-]+STARTS?\\b)[^\\]]*]", Pattern.CASE_INSENSITIVE);
    private static final Pattern OLD_DELTA_NAME = Pattern.compile(
        "\\{\\s*delta-pnj(?![\\p{L}\\p{N}_-])[^}\\n]*}?", Pattern.CASE_INSENSITIVE);

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
        Matcher oldName = OLD_DELTA_NAME.matcher(text);
        if (oldName.find()) {
            throw new IllegalArgumentException("Ancien nom de commande : " + oldName.group()
                + " -> la commande s'appelle maintenant npc-delta : {npc-delta public|secret nom : texte}.");
        }
    }
}
