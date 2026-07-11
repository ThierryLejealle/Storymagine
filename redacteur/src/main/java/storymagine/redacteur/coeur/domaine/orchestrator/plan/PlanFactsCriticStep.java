package storymagine.redacteur.coeur.domaine.orchestrator.plan;

import storymagine.redacteur.coeur.domaine.agent.plan.factscritic.PlanFactsCritic;
import storymagine.redacteur.coeur.domaine.agent.plan.factscritic.PlanFactsCriticInput;
import storymagine.redacteur.coeur.domaine.agent.plan.factscritic.PlanFactsCriticOutput;
import storymagine.redacteur.coeur.domaine.orchestrator.common.ScenarioFormatters;
import storymagine.redacteur.coeur.domaine.orchestrator.common.StoryFormatters;
import storymagine.redacteur.coeur.domaine.scenario.Chapter;
import storymagine.redacteur.coeur.domaine.scenario.Scenario;
import storymagine.redacteur.coeur.domaine.story.Story;

/** Activates PlanFactsCritic against the current chapter plan in Story. */
public class PlanFactsCriticStep {

    private final PlanFactsCritic agent;

    public PlanFactsCriticStep(PlanFactsCritic agent) {
        this.agent = agent;
    }

    public PlanFactsCriticOutput run(Scenario scenario, Chapter chapter, Story story) {
        String plan = story.currentChapter().orElseThrow().plan();
        String enrichedPlan = ScenarioFormatters.enrichPlanJson(plan, chapter.sequences());
        return agent.call(new PlanFactsCriticInput(
                enrichedPlan,
                chapter.description(),
                chapter.goal(),
                ScenarioFormatters.planCharactersText(chapter),
                String.join("\n", ScenarioFormatters.planChecks(scenario, chapter)),
                StoryFormatters.entityState(story.worldState()),
                scenario.config().language()
        ));
    }
}
