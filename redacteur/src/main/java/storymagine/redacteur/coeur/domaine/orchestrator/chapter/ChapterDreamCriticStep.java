package storymagine.redacteur.coeur.domaine.orchestrator.chapter;

import storymagine.redacteur.coeur.domaine.agent.chapter.dreamcritic.ChapterDreamCritic;
import storymagine.redacteur.coeur.domaine.agent.chapter.dreamcritic.ChapterDreamCriticInput;
import storymagine.redacteur.coeur.domaine.agent.chapter.dreamcritic.ChapterDreamCriticOutput;
import storymagine.redacteur.coeur.domaine.orchestrator.common.ScenarioFormatters;
import storymagine.redacteur.coeur.domaine.scenario.Scenario;

/** Activates ChapterDreamCritic on a DREAM chapter text. */
public class ChapterDreamCriticStep {

    private final ChapterDreamCritic agent;

    public ChapterDreamCriticStep(ChapterDreamCritic agent) {
        this.agent = agent;
    }

    public ChapterDreamCriticOutput run(String text, Scenario scenario) {
        return agent.call(new ChapterDreamCriticInput(
                text,
                ScenarioFormatters.bookGoal(scenario),
                "moyen"
        ));
    }
}
