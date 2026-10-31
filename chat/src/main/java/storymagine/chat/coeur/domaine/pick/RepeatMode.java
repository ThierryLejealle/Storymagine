package storymagine.chat.coeur.domaine.pick;

import java.util.Locale;

/**
 * How a "{repeat order|random [N] name}" block takes its paragraphs from the library : order (file
 * order, the first N) or random (N different ones drawn at random, so nothing comes out twice).
 * Without N the whole file is used (count 0). The block is repeated once per paragraph taken.
 */
public record RepeatMode(Order order, int count) {

    public enum Order { ORDER, RANDOM }

    /** The two or three words after "repeat" : "order name", "random name", "order 3 name", "random 3 name". */
    public static RepeatMode parse(String modeWord, String countWord) {
        Order order = switch (modeWord.strip().toLowerCase(Locale.ROOT)) {
            case "order"  -> Order.ORDER;
            case "random" -> Order.RANDOM;
            default -> throw new IllegalArgumentException("Mode de repeat inconnu : '" + modeWord + "' (order ou random)");
        };
        if (countWord == null) return new RepeatMode(order, 0);
        if (!countWord.matches("\\d{1,6}") || Integer.parseInt(countWord) < 1) {
            throw new IllegalArgumentException("Nombre de repeat invalide : '" + countWord + "' (entier superieur ou egal a 1)");
        }
        return new RepeatMode(order, Integer.parseInt(countWord));
    }

    public boolean takesEverything() {
        return count == 0;
    }

    /** How the draw is recorded for the 🎲 table. */
    public PickMode recordedMode() {
        return order == Order.ORDER ? PickMode.REPEAT_ORDER : PickMode.REPEAT_RANDOM;
    }
}
