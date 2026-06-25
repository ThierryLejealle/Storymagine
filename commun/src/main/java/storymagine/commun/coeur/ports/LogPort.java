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

    /** Summarises the pass/retry outcome for a group of critics. avg <= 0 means not applicable. */
    void scoresSummary(double avg, boolean passed, String retryHint);

    /** Called after each LLM generate() call with timing and token counts. */
    void llmCall(long ms, int tokIn, int tokOut, double tokPerSec);

    /** Saves the final plan text for a chapter (file log only — console NOOP). */
    void chapterPlan(String chapterTitle, String planText);

    /**
     * Called after all plan retries complete, only when ≥2 attempts actually ran.
     * Reports which attempt's plan was retained (highest average critic score).
     */
    default void planRetained(int bestAttempt, int totalAttempts, double bestScore) {}

    /** Saves the final text of one sequence for a chapter (file log only — console NOOP). */
    void sequenceText(String chapterTitle, int seqIdx, String text);

    /** Called after all chapters are generated — triggers final stats display / flush. */
    void sessionEnd();

    /** Logs a warning (inconsistency, unexpected state). Default is a no-op. */
    default void warn(String message) {}

    /**
     * Opens an LLM call trace (before generate()). Returns an opaque handle for llmCallClose.
     * Default implementation is a no-op returning an empty handle.
     */
    default String llmCallOpen(String agentName, int localNum, String systemPrompt, String userPrompt) {
        return "";
    }

    /**
     * Closes an LLM call trace (after generate()). handle = value returned by llmCallOpen.
     * Default implementation is a no-op.
     */
    default void llmCallClose(String handle, String response, long elapsedMs, int tokIn, int tokOut) {
    }

    /** No-op implementation for tests and contexts where logging is not needed. */
    LogPort NOOP = new LogPort() {
        @Override public void phaseHeader(String l, String d) {}
        @Override public void step(String n, long ms, String note) {}
        @Override public void critic(String n, double s, long ms, List<String> p) {}
        @Override public void scoresSummary(double avg, boolean passed, String hint) {}
        @Override public void llmCall(long ms, int tokIn, int tokOut, double tps) {}
        @Override public void chapterPlan(String t, String plan) {}
        @Override public void sequenceText(String t, int i, String txt) {}
        @Override public void sessionEnd() {}
    };
}
