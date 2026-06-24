package storymagine.redacteur.coeur.domaine.orchestrator.evaluate;

import storymagine.redacteur.coeur.domaine.agent.global.causalanalyzer.CausalAnalyzer;
import storymagine.redacteur.coeur.domaine.agent.global.causalanalyzer.CausalAnalyzerInput;
import storymagine.redacteur.coeur.domaine.agent.global.causalanalyzer.CausalAnalyzerOutput;
import storymagine.redacteur.coeur.domaine.story.Story;

import java.util.stream.Collectors;

/** Activates CausalAnalyzer on all chapter plans available in Story. */
public class CausalAnalyzerStep {

    private final CausalAnalyzer agent;

    public CausalAnalyzerStep(CausalAnalyzer agent) {
        this.agent = agent;
    }

    public CausalAnalyzerOutput run(Story story) {
        String plansText = story.chapters().stream()
                .filter(c -> c.plan() != null)
                .map(c -> "Chapitre " + c.id() + " :\n" + c.plan())
                .collect(Collectors.joining("\n\n"));
        return agent.call(new CausalAnalyzerInput(plansText));
    }
}
