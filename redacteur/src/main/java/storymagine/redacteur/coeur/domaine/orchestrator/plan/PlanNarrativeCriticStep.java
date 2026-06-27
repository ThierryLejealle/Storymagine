package storymagine.redacteur.coeur.domaine.orchestrator.plan;

import storymagine.redacteur.coeur.domaine.agent.plan.narrativecritic.PlanNarrativeCritic;
import storymagine.redacteur.coeur.domaine.agent.plan.narrativecritic.PlanNarrativeCriticInput;
import storymagine.redacteur.coeur.domaine.agent.plan.narrativecritic.PlanNarrativeCriticOutput;
import storymagine.redacteur.coeur.domaine.orchestrator.common.ScenarioFormatters;
import storymagine.redacteur.coeur.domaine.scenario.Chapter;
import storymagine.redacteur.coeur.domaine.scenario.Scenario;
import storymagine.redacteur.coeur.domaine.story.Story;

/** Activates PlanNarrativeCritic against the current chapter plan in Story. */
public class PlanNarrativeCriticStep {

    private final PlanNarrativeCritic agent;

    public PlanNarrativeCriticStep(PlanNarrativeCritic agent) {
        this.agent = agent;
    }

    public PlanNarrativeCriticOutput run(Scenario scenario, Chapter chapter, Story story) {
        String plan = story.currentChapter().orElseThrow().plan();
        return agent.call(new PlanNarrativeCriticInput(
                plan,
                ScenarioFormatters.bookGoal(scenario),
                chapter.goal()
        ));
    }
}
