package storymagine.chat.coeur.domaine.session;

/**
 * Chat's own context-usage estimate and compaction threshold — deliberately separate from
 * commun's SummaryBudget (shared with redacteur's Writer, which reserves only 1/8 of the window
 * because its prompt has much more competing for space: plan, characters, style rules...). Chat's
 * prompt is comparatively light (character sheet + scenario + current act), so raw turns can fill
 * much more of the window before folding into a summary — half of it here.
 */
public final class ChatContextBudget {

    private static final int CHARS_PER_TOKEN     = 4;
    private static final int TURNS_FRACTION      = 2;    // turns never take more than half the window...
    private static final int GENERATION_RESERVE  = 1024; // ...and always leave room for the fixed parts to grow and for the reply itself.

    private ChatContextBudget() {}

    /** Rough token estimate (chars/4) — no real tokenizer available without an LLM round-trip. */
    public static int estimateTokens(String text) {
        return text == null || text.isEmpty() ? 0 : text.length() / CHARS_PER_TOKEN;
    }

    /**
     * Estimated token budget left for raw (non-compacted) turns before folding into the summary.
     * Two caps apply, the tighter one wins : turns never exceed half the context window (so a
     * light scenario still gets to keep a lot of raw history), AND they never push the total
     * prompt (fixedPartsTokens : character sheet + scenario + current act + summary + formatting
     * rules) past the window minus a reserve for the model's own reply — a scenario with a big
     * character sheet, a long act, or a growing summary must compact sooner, not later.
     */
    public static int turnsBudget(int contextWindowTokens, int fixedPartsTokens) {
        int flatCap    = contextWindowTokens / TURNS_FRACTION;
        int dynamicCap = contextWindowTokens - fixedPartsTokens - GENERATION_RESERVE;
        return Math.max(0, Math.min(flatCap, dynamicCap));
    }
}
