package storymagine.chat.coeur.domaine.session;

/**
 * Business identifier of a turn within one run : 1 for the very first turn, +1 for each following
 * one (player, Npc or narrator alike), never reset by a compaction — only a rollback makes the next
 * number go back down. NONE (0) stands for "no turn yet" (an empty session, or a rollback to before
 * the first turn). Lives next to the turn as a field, never inside ChatTurn.text() : the model must
 * never read it (see NumberedTurn).
 */
public record TurnNumber(int value) implements Comparable<TurnNumber> {

    public static final TurnNumber NONE = new TurnNumber(0);

    public TurnNumber {
        if (value < 0) throw new IllegalArgumentException("Numero de tour negatif : " + value);
    }

    public static TurnNumber of(int value) {
        return new TurnNumber(value);
    }

    public TurnNumber next() {
        return new TurnNumber(value + 1);
    }

    @Override
    public int compareTo(TurnNumber other) {
        return Integer.compare(value, other.value);
    }
}
