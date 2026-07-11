package storymagine.redacteur.coeur.domaine.orchestrator.plan;

import storymagine.redacteur.coeur.domaine.agent.plan.continuitycritic.PlanContinuityCritic;
import storymagine.redacteur.coeur.domaine.agent.plan.continuitycritic.PlanContinuityCriticInput;
import storymagine.redacteur.coeur.domaine.agent.plan.continuitycritic.PlanContinuityCriticOutput;
import storymagine.redacteur.coeur.domaine.scenario.Chapter;
import storymagine.redacteur.coeur.domaine.scenario.Scenario;
import storymagine.redacteur.coeur.domaine.story.Story;

/** Activates PlanContinuityCritic against the current chapter plan in Story. */
public class PlanContinuityCriticStep {

    private final PlanContinuityCritic agent;

    public PlanContinuityCriticStep(PlanContinuityCritic agent) {
        this.agent = agent;
    }

    public PlanContinuityCriticOutput run(Scenario scenario, Chapter chapter, Story story) {
        String plan = story.currentChapter().orElseThrow().plan();
        return agent.call(new PlanContinuityCriticInput(
                plan,
                chapter.description(),
                chapter.goal(),
                story.summary(),
                scenario.config().language()
        ));
    }
}
