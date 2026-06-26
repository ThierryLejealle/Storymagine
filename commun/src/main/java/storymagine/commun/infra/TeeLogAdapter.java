package storymagine.commun.infra;

import storymagine.commun.coeur.ports.LogPort;

import java.util.List;

/**
 * Fan-out LogPort — forwards every call to each delegate in order.
 */
public class TeeLogAdapter implements LogPort {

    private final List<LogPort> delegates;

    public TeeLogAdapter(LogPort... delegates) {
        this.delegates = List.of(delegates);
    }

    @Override
    public void phaseHeader(String label, String detail) {
        delegates.forEach(d -> d.phaseHeader(label, detail));
    }

    @Override
    public void step(String name, long ms, String note) {
        delegates.forEach(d -> d.step(name, ms, note));
    }

    @Override
    public void critic(String name, double score, long ms, List<String> problems) {
        delegates.forEach(d -> d.critic(name, score, ms, problems));
    }

    @Override
    public void scoresSummary(double avg, boolean passed, String retryHint) {
        delegates.forEach(d -> d.scoresSummary(avg, passed, retryHint));
    }

    @Override
    public void llmCall(String agentLabel, long ms, int tokIn, int tokOut, double tokPerSec) {
        delegates.forEach(d -> d.llmCall(agentLabel, ms, tokIn, tokOut, tokPerSec));
    }

    @Override
    public void chapterPlan(String chapterTitle, String planText) {
        delegates.forEach(d -> d.chapterPlan(chapterTitle, planText));
    }

    @Override
    public void sequenceText(String chapterTitle, int seqIdx, String text) {
        delegates.forEach(d -> d.sequenceText(chapterTitle, seqIdx, text));
    }

    @Override
    public void sessionEnd() {
        delegates.forEach(LogPort::sessionEnd);
    }

    @Override
    public void planRetained(int bestAttempt, int totalAttempts, double bestScore) {
        delegates.forEach(d -> d.planRetained(bestAttempt, totalAttempts, bestScore));
    }

    @Override
    public String llmCallOpen(String agentName, int localNum,
                               String systemPrompt, String userPrompt) {
        String handle = "";
        for (LogPort d : delegates) {
            String h = d.llmCallOpen(agentName, localNum, systemPrompt, userPrompt);
            if (!h.isEmpty()) handle = h;
        }
        return handle;
    }

    @Override
    public void llmCallClose(String handle, String response, long elapsedMs,
                              int tokIn, int tokOut) {
        delegates.forEach(d -> d.llmCallClose(handle, response, elapsedMs, tokIn, tokOut));
    }
}
