package storymagine.redacteur.coeur.domaine.orchestrator.chapter;

import storymagine.redacteur.coeur.domaine.agent.chapter.narrativecritic.ChapterNarrativeCritic;
import storymagine.redacteur.coeur.domaine.agent.chapter.narrativecritic.ChapterNarrativeCriticInput;
import storymagine.redacteur.coeur.domaine.agent.chapter.narrativecritic.ChapterNarrativeCriticOutput;
import storymagine.redacteur.coeur.domaine.orchestrator.common.ScenarioFormatters;
import storymagine.redacteur.coeur.domaine.scenario.Chapter;
import storymagine.redacteur.coeur.domaine.scenario.Scenario;

/** Activates ChapterNarrativeCritic on a chapter text (IMPERATIVE chapters). */
public class ChapterNarrativeCriticStep {

    private final ChapterNarrativeCritic agent;

    public ChapterNarrativeCriticStep(ChapterNarrativeCritic agent) {
        this.agent = agent;
    }

    public ChapterNarrativeCriticOutput run(String text, Chapter chapter, Scenario scenario) {
        return agent.call(new ChapterNarrativeCriticInput(
                text,
                chapter.description(),
                chapter.goal(),
                ScenarioFormatters.bookGoal(scenario)
        ));
    }
}
