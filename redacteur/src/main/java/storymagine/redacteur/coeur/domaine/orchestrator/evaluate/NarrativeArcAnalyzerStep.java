package storymagine.redacteur.coeur.domaine.orchestrator.evaluate;

import storymagine.redacteur.coeur.domaine.agent.global.narrativearcanalyzer.NarrativeArcAnalyzer;
import storymagine.redacteur.coeur.domaine.agent.global.narrativearcanalyzer.NarrativeArcAnalyzerInput;
import storymagine.redacteur.coeur.domaine.agent.global.narrativearcanalyzer.NarrativeArcAnalyzerOutput;
import storymagine.redacteur.coeur.domaine.story.Story;
import storymagine.redacteur.coeur.domaine.story.WrittenChapter;

import java.util.stream.Collectors;

/** Activates NarrativeArcAnalyzer on all chapter plans available in Story. */
public class NarrativeArcAnalyzerStep {

    private final NarrativeArcAnalyzer agent;

    public NarrativeArcAnalyzerStep(NarrativeArcAnalyzer agent) {
        this.agent = agent;
    }

    public NarrativeArcAnalyzerOutput run(Story story) {
        String plansText = story.chapters().stream()
                .filter(c -> c.plan() != null)
                .map(c -> "Chapitre " + c.id() + " :\n" + c.plan())
                .collect(Collectors.joining("\n\n"));
        return agent.call(new NarrativeArcAnalyzerInput(plansText));
    }
}
