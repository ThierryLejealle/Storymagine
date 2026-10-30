package storymagine.chat.coeur.domaine.pick;

import java.util.Locale;

/**
 * How a "{pick mode name}" command draws from its library : random (one paragraph, repeats allowed),
 * unique (one paragraph never handed out yet), all (every paragraph, file order, no chance involved)
 * or a number N (N different paragraphs, drawn at random). label() is the mode as the author wrote it.
 */
public record PickMode(Kind kind, int count) {

    public enum Kind { RANDOM, UNIQUE, ALL, COUNT }

    public static final PickMode RANDOM = new PickMode(Kind.RANDOM, 1);
    public static final PickMode UNIQUE = new PickMode(Kind.UNIQUE, 1);
    public static final PickMode ALL    = new PickMode(Kind.ALL, 0);

    public static PickMode ofCount(int count) {
        if (count < 1) throw new IllegalArgumentException("Un nombre de tirages doit etre superieur ou egal a 1 : " + count);
        return new PickMode(Kind.COUNT, count);
    }

    /** "random", "unique", "all" or a whole number >= 1, any case ; IllegalArgumentException otherwise. */
    public static PickMode parse(String word) {
        String w = word.strip().toLowerCase(Locale.ROOT);
        switch (w) {
            case "random": return RANDOM;
            case "unique": return UNIQUE;
            case "all":    return ALL;
            default:
                if (w.matches("\\d{1,6}")) return ofCount(Integer.parseInt(w));
                throw new IllegalArgumentException("Type de pick inconnu : '" + word + "' (random, unique, all ou un nombre)");
        }
    }

    public String label() {
        return switch (kind) {
            case RANDOM -> "random";
            case UNIQUE -> "unique";
            case ALL    -> "all";
            case COUNT  -> Integer.toString(count);
        };
    }
}
