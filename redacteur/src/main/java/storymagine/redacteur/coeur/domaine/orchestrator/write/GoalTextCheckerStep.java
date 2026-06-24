package storymagine.redacteur.coeur.domaine.orchestrator.write;

import storymagine.redacteur.coeur.domaine.agent.writer.goaltextchecker.GoalTextChecker;
import storymagine.redacteur.coeur.domaine.agent.writer.goaltextchecker.GoalTextCheckerInput;
import storymagine.redacteur.coeur.domaine.agent.writer.goaltextchecker.GoalTextCheckerOutput;
import storymagine.redacteur.coeur.domaine.orchestrator.common.ScenarioFormatters;
import storymagine.redacteur.coeur.domaine.scenario.Chapter;
import storymagine.redacteur.coeur.domaine.scenario.Scenario;

/** Activates GoalTextChecker to verify the written chapter text achieves its narrative goal. */
public class GoalTextCheckerStep {

    private final GoalTextChecker agent;

    public GoalTextCheckerStep(GoalTextChecker agent) {
        this.agent = agent;
    }

    public GoalTextCheckerOutput run(String text, Chapter chapter, Scenario scenario) {
        return agent.call(new GoalTextCheckerInput(
                text,
                chapter.goal(),
                ScenarioFormatters.bookGoal(scenario)
        ));
    }
}
