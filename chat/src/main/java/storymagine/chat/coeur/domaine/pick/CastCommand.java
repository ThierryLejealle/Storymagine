package storymagine.chat.coeur.domaine.pick;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A "{cast NAME [unique] [reuse-possible] [(jack, mia, john)]}" line : it picks one character of the cast
 * and keeps it in the variable NAME, written "%NAME%" in the text after it (see CastResolver). By default
 * the character is different from the ones the other variables hold at that moment ; reusePossible lifts
 * that. unique means this variable never takes twice the same character (in a "{repeat}", a different one
 * at each copy). candidates are the characters the author listed, one word each (see Cast.findByName) ;
 * none listed means the whole cast. source is the line as written, to point at it in an error message.
 */
public record CastCommand(String variable, boolean unique, boolean reusePossible, List<String> candidates, String source) {

    public static final Pattern VARIABLE_NAME = Pattern.compile("[A-Za-z][A-Za-z0-9_]*");
    private static final Pattern START = Pattern.compile("\\{\\s*cast(?![\\p{L}\\p{N}_-])", Pattern.CASE_INSENSITIVE);
    private static final Pattern LINE = Pattern.compile(
        "^\\{\\s*cast\\s+([^\\s(}]+)((?:\\s+[^\\s(}]+)*)\\s*(?:\\(([^)]*)\\))?\\s*}$", Pattern.CASE_INSENSITIVE);
    private static final String SYNTAX = "{cast NOM [unique] [reuse-possible] [(jack, mia, john)]}";

    public CastCommand {
        candidates = List.copyOf(candidates);
    }

    /** True if the text holds a "{cast" anywhere, well formed or not. */
    public static boolean appearsIn(String text) {
        return START.matcher(text).find();
    }

    /**
     * The command a line holds, empty when the line has no "{cast". A "{cast" that is not alone on its
     * line, or badly written, is an IllegalArgumentException : never silently left in the text.
     */
    public static Optional<CastCommand> parseLine(String line) {
        String stripped = line.strip();
        if (!START.matcher(stripped).find()) return Optional.empty();
        Matcher m = LINE.matcher(stripped);
        if (!m.matches()) {
            throw new IllegalArgumentException("Commande cast invalide : " + stripped + " (attendu, seul sur sa ligne : " + SYNTAX + ")");
        }
        String variable = m.group(1);
        if (!VARIABLE_NAME.matcher(variable).matches()) {
            throw new IllegalArgumentException("Nom de variable invalide : '" + variable + "' dans " + stripped
                + " (une lettre puis lettres, chiffres ou _)");
        }
        boolean unique = false;
        boolean reusePossible = false;
        for (String keyword : m.group(2).strip().isEmpty() ? new String[0] : m.group(2).strip().split("\\s+")) {
            switch (keyword.toLowerCase(Locale.ROOT)) {
                case "unique" -> unique = true;
                case "reuse-possible" -> reusePossible = true;
                default -> throw new IllegalArgumentException("Mot cle inconnu '" + keyword + "' dans " + stripped
                    + " (mots cles : unique, reuse-possible)");
            }
        }
        return Optional.of(new CastCommand(variable, unique, reusePossible, candidatesOf(m.group(3), stripped), stripped));
    }

    /** The words of "(jack, mia)" : one character per word ; null (no parentheses) means no list. */
    static List<String> candidatesOf(String insideParentheses, String where) {
        if (insideParentheses == null) return List.of();
        List<String> words = new ArrayList<>();
        for (String entry : insideParentheses.split(",", -1)) {
            String word = entry.strip();
            if (word.isEmpty()) {
                throw new IllegalArgumentException("Liste de personnages vide ou avec une virgule en trop dans " + where);
            }
            if (word.matches(".*\\s.*")) {
                throw new IllegalArgumentException("Un personnage s'ecrit avec un seul mot (son identifiant ou un mot de son nom) : '"
                    + word + "' dans " + where);
            }
            words.add(word);
        }
        return words;
    }
}
