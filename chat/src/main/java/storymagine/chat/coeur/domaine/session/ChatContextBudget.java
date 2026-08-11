package storymagine.chat.coeur.domaine.session;

/**
 * Chat's own context-usage estimate and compaction threshold — deliberately separate from
 * commun's SummaryBudget (shared with redacteur's Writer, which reserves only 1/8 of the window
 * because its prompt has much more competing for space: plan, characters, style rules...). Chat's
 * prompt is comparatively light (character sheet + scenario + current act), so raw turns can fill
 * much more of the window before folding into a summary.
 *
 * Two independent percentages of the window, not derived from one another (raised 2026-07-22,
 * after 50%/~10% felt like a cliff — losing a lot of raw detail in one big fold) : TRIGGER_PERCENT
 * is how full raw turns get before compaction kicks in at all, KEEP_PERCENT is how much survives
 * right after it does. Keeping KEEP_PERCENT well below TRIGGER_PERCENT still leaves headroom
 * before the next compaction, but a smaller gap between the two means each individual fold handles
 * less text (gentler on the summary's fidelity) at the cost of firing more often overall — the
 * trade-off asked for.
 */
public final class ChatContextBudget {

    private static final int CHARS_PER_TOKEN    = 4;
    private static final int TRIGGER_PERCENT    = 70;   // compact once raw turns reach this % of the window...
    private static final int KEEP_PERCENT       = 25;   // ...and aim to keep about this % raw right after.
    private static final int GENERATION_RESERVE = 1024; // always leave room for the fixed parts to grow and for the reply itself.

    private ChatContextBudget() {}

    /** Rough token estimate (chars/4) — no real tokenizer available without an LLM round-trip. */
    public static int estimateTokens(String text) {
        return text == null || text.isEmpty() ? 0 : text.length() / CHARS_PER_TOKEN;
    }

    /**
     * Estimated token budget left for raw (non-compacted) turns before folding into the summary.
     * Two caps apply, the tighter one wins : turns never exceed TRIGGER_PERCENT of the context
     * window (so a light scenario still gets to keep a lot of raw history), AND they never push
     * the total prompt (fixedPartsTokens : character sheet + scenario + current act + summary +
     * formatting rules) past the window minus a reserve for the model's own reply — a scenario
     * with a big character sheet, a long act, or a growing summary must compact sooner, not later.
     */
    public static int turnsBudget(int contextWindowTokens, int fixedPartsTokens) {
        int flatCap    = contextWindowTokens * TRIGGER_PERCENT / 100;
        int dynamicCap = contextWindowTokens - fixedPartsTokens - GENERATION_RESERVE;
        return Math.max(0, Math.min(flatCap, dynamicCap));
    }

    /**
     * Target amount of raw turn tokens to still be sitting there right after a compaction —
     * KEEP_PERCENT of the window, but never more than triggerThreshold itself (keeping more than
     * what's allowed to trigger compaction would just re-trigger it immediately, and if a tight
     * dynamicCap already pushed the trigger threshold below the flat KEEP_PERCENT target, keeping
     * MUST shrink to match, same reasoning as turnsBudget's own two caps).
     */
    public static int keepTokensTarget(int contextWindowTokens, int triggerThreshold) {
        int flatTarget = contextWindowTokens * KEEP_PERCENT / 100;
        return Math.max(0, Math.min(flatTarget, triggerThreshold));
    }
}
