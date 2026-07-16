package storymagine.commun.coeur.ports;

import java.util.List;

/**
 * Observability port for story generation workflow events.
 */
public interface LogPort {

    /** Marks the start of a named phase (chapter header, PLAN attempt, WRITE sequence, EVAL). */
    void phaseHeader(String label, String detail);

    /** Logs completion of a single agent step with optional note (e.g. "-> 612 mots"). */
    void step(String name, long ms, String note);

    /** Logs a critic result: score, timing, and problems (empty list = passed cleanly). */
    void critic(String name, double score, long ms, List<String> problems);

    /**
     * Summarises the pass/retry outcome for a group of critics. avg <= 0 means not applicable.
     * avgThreshold is the average score required to pass, shown alongside avg for readability.
     * minScore is the lowest individual critic score, shown alongside eliminationThreshold so
     * the elimination check (any critic below eliminationThreshold forces a retry) is visible.
     * passed reflects the score gate alone (average/elimination), never overridden by forcedRetry.
     * forcedRetry is true when a retry happens despite passed being true, for a reason other than
     * the score gate (e.g. ChapterPlanWorkflow's strict-first-attempt rule : any remark at all
     * forces one retry even though scores passed) — always false for workflows with no such rule.
     * Adapters render three distinct outcomes : passed=true/forcedRetry=false → PASS (green) ;
     * passed=true/forcedRetry=true → REFINE (orange, retrying only because remarks remain) ;
     * passed=false → RETRY (red, a real score failure, forcedRetry irrelevant in that case).
     */
    void scoresSummary(double avg, double avgThreshold, double minScore, double eliminationThreshold,
                        boolean passed, boolean forcedRetry, String retryHint);

    /**
     * Called after each LLM generate() call with agent label, timing, token counts, and whether
     * reasoning ("thinking") was actually requested for this call (null = model has no such capability).
     */
    void llmCall(String agentLabel, long ms, int tokIn, int tokOut, double tokPerSec, Boolean think);

    /** Saves the final plan text for a chapter (file log only — console NOOP). */
    void chapterPlan(String chapterTitle, String planText);

    /**
     * Called after all plan retries complete, only when ≥2 attempts actually ran.
     * Reports which attempt's plan was retained (highest average critic score).
     */
    default void planRetained(int bestAttempt, int totalAttempts, double bestScore) {}

    /**
     * Called after all sequence critique passes, only when ≥2 passes actually ran.
     * Reports which pass's text was retained (highest average critic score).
     */
    default void sequenceRetained(int bestPass, int totalPasses, double bestScore) {}

    /**
     * Called after all chapter critique attempts, only when ≥2 attempts actually ran.
     * Reports which attempt's sequences were retained (highest average critic score).
     */
    default void chapterRetained(int bestPass, int totalPasses, double bestScore) {}

    /** Saves the final text of one sequence for a chapter (file log only — console NOOP). */
    void sequenceText(String chapterTitle, int seqIdx, String text);

    /** Called after all chapters are generated — triggers final stats display / flush. */
    void sessionEnd();

    /** Logs a warning (inconsistency, unexpected state). Default is a no-op. */
    default void warn(String message) {}

    /** Logs an informational note (expected behaviour worth surfacing, not a problem). Default is a no-op. */
    default void info(String message) {}

    /**
     * Opens an LLM call trace (before generate()). Returns an opaque handle for llmCallClose.
     * think = whether reasoning was requested for this call (null = model has no such capability).
     * Default implementation is a no-op returning an empty handle.
     */
    default String llmCallOpen(String agentName, int localNum, String systemPrompt, String userPrompt, Boolean think) {
        return "";
    }

    /**
     * Closes an LLM call trace (after generate()). handle = value returned by llmCallOpen.
     * thinking = the reasoning text actually returned, if any ("" when none — either not
     * requested, or the model returned none this time). Default implementation is a no-op.
     */
    default void llmCallClose(String handle, String response, String thinking, long elapsedMs, int tokIn, int tokOut) {
    }

    /** No-op implementation for tests and contexts where logging is not needed. */
    LogPort NOOP = new LogPort() {
        @Override public void phaseHeader(String l, String d) {}
        @Override public void step(String n, long ms, String note) {}
        @Override public void critic(String n, double s, long ms, List<String> p) {}
        @Override public void scoresSummary(double avg, double avgThreshold, double minScore, double eliminationThreshold, boolean passed, boolean forcedRetry, String hint) {}
        @Override public void llmCall(String lbl, long ms, int tokIn, int tokOut, double tps, Boolean think) {}
        @Override public void chapterPlan(String t, String plan) {}
        @Override public void sequenceText(String t, int i, String txt) {}
        @Override public void sessionEnd() {}
    };
}
