package storymagine.redacteur.coeur.domaine.orchestrator.chapter;

import storymagine.redacteur.coeur.domaine.agent.chapter.goalcritic.ChapterGoalCritic;
import storymagine.redacteur.coeur.domaine.agent.chapter.goalcritic.ChapterGoalCriticInput;
import storymagine.redacteur.coeur.domaine.agent.chapter.goalcritic.ChapterGoalCriticOutput;
import storymagine.redacteur.coeur.domaine.orchestrator.common.ScenarioFormatters;
import storymagine.redacteur.coeur.domaine.scenario.Chapter;
import storymagine.redacteur.coeur.domaine.scenario.Scenario;

/** Activates ChapterGoalCritic to verify the written chapter text achieves its narrative goal. */
public class ChapterGoalCriticStep {

    private final ChapterGoalCritic agent;

    public ChapterGoalCriticStep(ChapterGoalCritic agent) {
        this.agent = agent;
    }

    public ChapterGoalCriticOutput run(String text, Chapter chapter, Scenario scenario) {
        return agent.call(new ChapterGoalCriticInput(
                text,
                chapter.goal(),
                ScenarioFormatters.bookGoal(scenario)
        ));
    }
}
