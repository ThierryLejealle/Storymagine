package storymagine.redacteur.coeur.domaine.orchestrator.plan;

import storymagine.redacteur.coeur.domaine.agent.plan.dramacritic.PlanDramaCritic;
import storymagine.redacteur.coeur.domaine.agent.plan.dramacritic.PlanDramaCriticInput;
import storymagine.redacteur.coeur.domaine.agent.plan.dramacritic.PlanDramaCriticOutput;
import storymagine.redacteur.coeur.domaine.scenario.Chapter;
import storymagine.redacteur.coeur.domaine.scenario.Scenario;
import storymagine.redacteur.coeur.domaine.story.Story;

/** Activates PlanDramaCritic against the current chapter plan in Story. */
public class PlanDramaCriticStep {

    private final PlanDramaCritic agent;

    public PlanDramaCriticStep(PlanDramaCritic agent) {
        this.agent = agent;
    }

    public PlanDramaCriticOutput run(Scenario scenario, Chapter chapter, Story story) {
        String plan = story.currentChapter().orElseThrow().plan();
        return agent.call(new PlanDramaCriticInput(
                plan,
                chapter.description(),
                chapter.goal(),
                scenario.config().language()
        ));
    }
}
