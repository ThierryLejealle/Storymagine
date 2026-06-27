package storymagine.redacteur.coeur.domaine.orchestrator.chapter;

import storymagine.redacteur.coeur.domaine.agent.chapter.goaltextcritic.GoalTextCritic;
import storymagine.redacteur.coeur.domaine.agent.chapter.goaltextcritic.GoalTextCriticInput;
import storymagine.redacteur.coeur.domaine.agent.chapter.goaltextcritic.GoalTextCriticOutput;
import storymagine.redacteur.coeur.domaine.orchestrator.common.ScenarioFormatters;
import storymagine.redacteur.coeur.domaine.scenario.Chapter;
import storymagine.redacteur.coeur.domaine.scenario.Scenario;

/** Activates GoalTextCritic to verify the written chapter text achieves its narrative goal. */
public class GoalTextCriticStep {

    private final GoalTextCritic agent;

    public GoalTextCriticStep(GoalTextCritic agent) {
        this.agent = agent;
    }

    public GoalTextCriticOutput run(String text, Chapter chapter, Scenario scenario) {
        return agent.call(new GoalTextCriticInput(
                text,
                chapter.goal(),
                ScenarioFormatters.bookGoal(scenario)
        ));
    }
}
