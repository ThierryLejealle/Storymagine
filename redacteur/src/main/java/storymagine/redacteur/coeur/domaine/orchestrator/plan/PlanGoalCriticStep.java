package storymagine.redacteur.coeur.domaine.orchestrator.plan;

import storymagine.redacteur.coeur.domaine.agent.plan.goalcritic.PlanGoalCritic;
import storymagine.redacteur.coeur.domaine.agent.plan.goalcritic.PlanGoalCriticInput;
import storymagine.redacteur.coeur.domaine.agent.plan.goalcritic.PlanGoalCriticOutput;
import storymagine.redacteur.coeur.domaine.orchestrator.common.ScenarioFormatters;
import storymagine.redacteur.coeur.domaine.scenario.Chapter;
import storymagine.redacteur.coeur.domaine.scenario.Scenario;
import storymagine.redacteur.coeur.domaine.story.Story;

/** Activates PlanGoalCritic against the current chapter plan in Story. */
public class PlanGoalCriticStep {

    private final PlanGoalCritic agent;

    public PlanGoalCriticStep(PlanGoalCritic agent) {
        this.agent = agent;
    }

    public PlanGoalCriticOutput run(Scenario scenario, Chapter chapter, Story story) {
        String plan = story.currentChapter().orElseThrow().plan();
        return agent.call(new PlanGoalCriticInput(
                plan,
                chapter.description(),
                chapter.goal(),
                ScenarioFormatters.sequenceDirectivesBlock(chapter.sequences()),
                ScenarioFormatters.bookGoal(scenario),
                scenario.config().language()
        ));
    }
}
