package storymagine.redacteur.coeur.domaine.orchestrator.write;

import storymagine.redacteur.coeur.domaine.agent.sequence.planfidelitycritic.PlanFidelityCritic;
import storymagine.redacteur.coeur.domaine.agent.sequence.planfidelitycritic.PlanFidelityCriticInput;
import storymagine.redacteur.coeur.domaine.agent.sequence.planfidelitycritic.PlanFidelityCriticOutput;

import java.util.ArrayList;
import java.util.List;

/** Activates PlanFidelityCritic to verify all planned beats are developed in the written text. */
public class PlanFidelityCriticStep {

    private final PlanFidelityCritic agent;

    public PlanFidelityCriticStep(PlanFidelityCritic agent) {
        this.agent = agent;
    }

    public PlanFidelityCriticOutput run(String sequenceText, List<String> beats) {
        return agent.call(new PlanFidelityCriticInput(beats, sequenceText));
    }

    /** Extracts the numbered beats from a formatted sequence plan string (BEATS : section). */
    public static List<String> extractBeats(String sequencePlan) {
        if (sequencePlan == null || sequencePlan.isBlank()) return List.of();
        List<String> beats = new ArrayList<>();
        boolean inBeats = false;
        for (String line : sequencePlan.split("\n")) {
            String t = line.trim();
            if (t.equalsIgnoreCase("BEATS :") || t.equalsIgnoreCase("BEATS:")) {
                inBeats = true;
                continue;
            }
            if (inBeats) {
                if (t.isBlank()) continue;
                if (t.matches("^\\d+\\..*")) {
                    beats.add(t.replaceFirst("^\\d+\\.\\s*", ""));
                } else {
                    inBeats = false;
                }
            }
        }
        return beats;
    }
}
