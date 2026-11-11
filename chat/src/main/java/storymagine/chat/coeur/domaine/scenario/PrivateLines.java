package storymagine.chat.coeur.domaine.scenario;

import storymagine.chat.coeur.domaine.activity.ActivityResolver;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The "{private Name} text" lines and "{private Name}" ... "{end-private}" blocks of a scene : text that only
 * the named characters read. Everybody else's prompt is built without it (visibleTo), so a character cannot
 * act on what only another one was told. A "{private ...}" with text after it on the same line covers that
 * line only ; alone on its line it opens a block closed by "{end-private}". Blocks do not nest. The text
 * keeps the commands until the prompt is built, so the analysis agents, which are not characters, still see
 * the whole scene. A private text never holds a "[NEXT ACT]" (the character who ends the scene must
 * read it) nor a "[line]" shown to the player.
 */
public final class PrivateLines {

    private static final Pattern OPEN = Pattern.compile("^[ \\t]*\\{private[ \\t]+([^}]*)}[ \\t]*(.*)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CLOSE = Pattern.compile("^[ \\t]*\\{end-private}[ \\t]*$", Pattern.CASE_INSENSITIVE);
    /** Either command anywhere in a line : used to find the ones that are not at the start of their line. */
    private static final Pattern ANY = Pattern.compile("\\{\\s*(?:end-)?private(?![\\p{L}\\p{N}_-])", Pattern.CASE_INSENSITIVE);
    private static final Pattern PLAYER_LINE = Pattern.compile("^[ \\t]*\\[.*][ \\t]*$");

    private PrivateLines() {}

    public static boolean appearsIn(String text) {
        return text != null && ANY.matcher(text).find();
    }

    /**
     * The text as the given character reads it : the commands are cut out, and so is every private text not
     * addressed to this character. Text without a command comes back unchanged.
     * @throws IllegalArgumentException malformed command or unknown character name (see validate)
     */
    public static String visibleTo(String text, Cast cast, Npc reader) {
        if (!appearsIn(text)) return text;
        List<String> kept = new ArrayList<>();
        scan(text, cast, (line, audience) -> {
            if (audience == null || audience.contains(reader.id())) kept.add(line);
        });
        return String.join("\n", kept);
    }

    /**
     * Checks the commands of a text : each at the start of its line, every name a character of the cast
     * (names holding a "%" are variables drawn later and are not checked), blocks closed and not nested, no
     * "[NEXT ACT]" nor "[line]" inside. Messages give the line number in this text.
     * @throws IllegalArgumentException the first problem found
     */
    public static void validate(String text, Cast cast) {
        if (appearsIn(text)) scan(text, cast, (line, audience) -> {});
    }

    /** Receives each text line with who reads it (null = everybody) ; the command lines themselves are not sent. */
    private interface Sink {
        void accept(String line, Set<String> audience);
    }

    private static void scan(String text, Cast cast, Sink sink) {
        String[] lines = text.split("\n", -1);
        Set<String> blockAudience = null;
        int blockOpenedAt = 0;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int number = i + 1;
            Matcher open = OPEN.matcher(line);
            if (open.matches()) {
                if (blockAudience != null) {
                    throw new IllegalArgumentException("ligne " + number + " : {private} dans un bloc {private} deja ouvert a la ligne "
                        + blockOpenedAt + " (pas d'imbrication ; fermer d'abord avec {end-private}).");
                }
                Set<String> audience = audience(open.group(1), cast, number);
                String rest = open.group(2).strip();
                if (rest.isEmpty()) {
                    blockAudience = audience;
                    blockOpenedAt = number;
                } else {
                    requireReadableOnlyByCharacters(rest, number);
                    sink.accept(rest, audience);
                }
                continue;
            }
            if (CLOSE.matcher(line).matches()) {
                if (blockAudience == null) {
                    throw new IllegalArgumentException("ligne " + number + " : {end-private} sans {private Nom} ouvert avant.");
                }
                blockAudience = null;
                continue;
            }
            if (ANY.matcher(line).find()) {
                throw new IllegalArgumentException("ligne " + number + " : commande {private} ou {end-private} mal placee : " + line.strip()
                    + " (attendu au debut de la ligne : {private Nom} texte, ou {private Nom} seul puis {end-private}).");
            }
            if (blockAudience != null && line.startsWith("#")) {
                throw new IllegalArgumentException("ligne " + number + " : le bloc {private} ouvert a la ligne " + blockOpenedAt
                    + " doit etre ferme par {end-private} avant le titre suivant.");
            }
            if (blockAudience != null) requireReadableOnlyByCharacters(line, number);
            sink.accept(line, blockAudience);
        }
        if (blockAudience != null) {
            throw new IllegalArgumentException("{private} ouvert a la ligne " + blockOpenedAt + " : {end-private} manque avant la fin du texte ou du titre.");
        }
    }

    private static Set<String> audience(String namesText, Cast cast, int number) {
        if (namesText.strip().isEmpty()) {
            throw new IllegalArgumentException("ligne " + number + " : {private} sans nom (attendu : {private Nom} ou {private Nom1, Nom2}).");
        }
        if (namesText.indexOf('%') >= 0) return Set.of(); // a drawn character : known once the game starts
        Set<String> ids = new HashSet<>();
        try {
            for (Npc npc : ActivityResolver.resolveNames(cast, namesText)) ids.add(npc.id());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("ligne " + number + " : {private " + namesText.strip() + "} : " + e.getMessage(), e);
        }
        return ids;
    }

    private static void requireReadableOnlyByCharacters(String line, int number) {
        if (NextActMarker.isPresentIn(line)) {
            throw new IllegalArgumentException("ligne " + number + " : [NEXT ACT] ne peut pas etre dans un texte {private} : "
                + "le personnage qui termine la scene doit pouvoir le lire.");
        }
        if (PLAYER_LINE.matcher(line).matches()) {
            throw new IllegalArgumentException("ligne " + number + " : une ligne [entre crochets] est montree au joueur, "
                + "elle ne peut pas etre dans un texte {private}.");
        }
    }
}
