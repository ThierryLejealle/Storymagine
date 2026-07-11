package storymagine.commun.coeur.domaine.text;

/**
 * Shared "story so far" budget, derived from the model's context window — used both by
 * Writer (as a character budget for its storySoFar/summary slot) and by the running-summary
 * compaction threshold, so the two never drift out of sync.
 */
public final class SummaryBudget {

    private static final int FRACTION        = 8; // 1/8 of the context window
    private static final int CHARS_PER_TOKEN = 4;
    private static final int CHARS_PER_WORD  = 6; // French average

    private SummaryBudget() {
    }

    public static int charBudget(int contextWindowTokens) {
        return contextWindowTokens * CHARS_PER_TOKEN / FRACTION;
    }

    public static int wordBudget(int contextWindowTokens) {
        return charBudget(contextWindowTokens) / CHARS_PER_WORD;
    }
}
