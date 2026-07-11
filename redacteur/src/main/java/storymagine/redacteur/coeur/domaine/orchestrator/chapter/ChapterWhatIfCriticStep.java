package storymagine.redacteur.coeur.domaine.orchestrator.chapter;

import storymagine.redacteur.coeur.domaine.agent.chapter.whatifcritic.ChapterWhatIfCritic;
import storymagine.redacteur.coeur.domaine.agent.chapter.whatifcritic.ChapterWhatIfCriticInput;
import storymagine.redacteur.coeur.domaine.agent.chapter.whatifcritic.ChapterWhatIfCriticOutput;
import storymagine.redacteur.coeur.domaine.orchestrator.common.ScenarioFormatters;
import storymagine.redacteur.coeur.domaine.scenario.Chapter;
import storymagine.redacteur.coeur.domaine.scenario.Scenario;

/** Activates ChapterWhatIfCritic on a WHAT_IF chapter text. */
public class ChapterWhatIfCriticStep {

    private final ChapterWhatIfCritic agent;

    public ChapterWhatIfCriticStep(ChapterWhatIfCritic agent) {
        this.agent = agent;
    }

    public ChapterWhatIfCriticOutput run(String text, Scenario scenario, Chapter chapter) {
        return agent.call(new ChapterWhatIfCriticInput(
                text,
                String.join("\n", ScenarioFormatters.writerChecks(scenario, chapter))
        ));
    }
}
