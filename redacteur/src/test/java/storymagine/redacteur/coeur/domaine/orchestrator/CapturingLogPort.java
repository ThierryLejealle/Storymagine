package storymagine.redacteur.coeur.domaine.orchestrator;

import storymagine.commun.coeur.ports.LogPort;

import java.util.ArrayList;
import java.util.List;

/**
 * Test double for LogPort — records all calls for assertion.
 */
public class CapturingLogPort implements LogPort {

    public final List<String> phases  = new ArrayList<>();
    public final List<String> steps   = new ArrayList<>();
    public final List<String> critics = new ArrayList<>();
    public final List<String> scores  = new ArrayList<>();

    @Override
    public void phaseHeader(String label, String detail) {
        phases.add(label);
    }

    @Override
    public void step(String name, long ms, String note) {
        steps.add(name);
    }

    @Override
    public void critic(String name, double score, long ms, List<String> problems) {
        critics.add(name + ":" + String.format("%.2f", score));
    }

    @Override
    public void scoresSummary(double avg, boolean passed, String retryHint) {
        scores.add(passed ? "PASS" : "RETRY");
    }

    @Override public void llmCall(String lbl, long ms, int tokIn, int tokOut, double tps) {}
    @Override public void chapterPlan(String title, String plan) {}
    @Override public void sequenceText(String title, int idx, String text) {}
    @Override public void sessionEnd() {}

    public boolean hasPhase(String fragment) {
        return phases.stream().anyMatch(p -> p.contains(fragment));
    }

    public boolean hasStep(String name) {
        return steps.contains(name);
    }

    public boolean hasCritic(String name) {
        return critics.stream().anyMatch(c -> c.startsWith(name + ":"));
    }

    public boolean hasScore(String outcome) {
        return scores.contains(outcome);
    }
}
