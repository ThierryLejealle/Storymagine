package storymagine.redacteur.coeur.domaine.orchestrator.chapter;

import storymagine.redacteur.coeur.domaine.agent.chapter.coherencecritic.ChapterCoherenceCritic;
import storymagine.redacteur.coeur.domaine.agent.chapter.coherencecritic.ChapterCoherenceCriticInput;
import storymagine.redacteur.coeur.domaine.agent.chapter.coherencecritic.ChapterCoherenceCriticOutput;
import storymagine.redacteur.coeur.domaine.orchestrator.common.ScenarioFormatters;
import storymagine.redacteur.coeur.domaine.scenario.Chapter;
import storymagine.redacteur.coeur.domaine.scenario.Scenario;

/** Activates ChapterCoherenceCritic on a chapter text (IMPERATIVE and WHAT_IF chapters). */
public class ChapterCoherenceCriticStep {

    private final ChapterCoherenceCritic agent;

    public ChapterCoherenceCriticStep(ChapterCoherenceCritic agent) {
        this.agent = agent;
    }

    public ChapterCoherenceCriticOutput run(String text, Scenario scenario, Chapter chapter) {
        return agent.call(new ChapterCoherenceCriticInput(
                text,
                String.join("\n", ScenarioFormatters.writerChecks(scenario, chapter))
        ));
    }
}
