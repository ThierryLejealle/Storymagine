package storymagine.redacteur.coeur.domaine.orchestrator.plan;

import storymagine.redacteur.coeur.domaine.agent.plan.goalchecker.GoalPlanChecker;
import storymagine.redacteur.coeur.domaine.agent.plan.goalchecker.GoalPlanCheckerInput;
import storymagine.redacteur.coeur.domaine.agent.plan.goalchecker.GoalPlanCheckerOutput;
import storymagine.redacteur.coeur.domaine.orchestrator.common.ScenarioFormatters;
import storymagine.redacteur.coeur.domaine.scenario.Chapter;
import storymagine.redacteur.coeur.domaine.scenario.Scenario;
import storymagine.redacteur.coeur.domaine.story.Story;

/** Activates GoalPlanChecker against the current chapter plan in Story. */
public class GoalPlanCheckerStep {

    private final GoalPlanChecker agent;

    public GoalPlanCheckerStep(GoalPlanChecker agent) {
        this.agent = agent;
    }

    public GoalPlanCheckerOutput run(Scenario scenario, Chapter chapter, Story story) {
        String plan = story.currentChapter().orElseThrow().plan();
        return agent.call(new GoalPlanCheckerInput(
                plan,
                chapter.goal(),
                ScenarioFormatters.bookGoal(scenario)
        ));
    }
}
