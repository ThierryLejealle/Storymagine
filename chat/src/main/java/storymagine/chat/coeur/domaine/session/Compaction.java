package storymagine.chat.coeur.domaine.session;

/**
 * One fold of the oldest turns into the running summary. createdAfter (R) is the last turn that
 * existed when it happened — it places the compaction in the run's sequence, right after that turn.
 * foldedUpTo (F) is the last turn actually folded into the summary : the turns F+1..R stay verbatim
 * in the live window. F is normally below R ; they are equal only for a degenerate empty window.
 * summary is cumulative (it already contains the previous summary), so restoring a run at any given
 * turn needs exactly one compaction : the most recent one with createdAfter at or before that turn.
 */
public record Compaction(TurnNumber createdAfter, TurnNumber foldedUpTo, String summary) {

    public Compaction {
        if (foldedUpTo.compareTo(createdAfter) > 0) {
            throw new IllegalArgumentException(
                "foldedUpTo (" + foldedUpTo.value() + ") ne peut pas depasser createdAfter (" + createdAfter.value() + ")");
        }
    }
}
