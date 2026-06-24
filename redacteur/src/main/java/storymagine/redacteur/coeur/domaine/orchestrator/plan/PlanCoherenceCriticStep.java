package storymagine.redacteur.coeur.domaine.orchestrator.plan;

import storymagine.redacteur.coeur.domaine.agent.plan.plancoherencecritic.PlanCoherenceCritic;
import storymagine.redacteur.coeur.domaine.agent.plan.plancoherencecritic.PlanCoherenceCriticInput;
import storymagine.redacteur.coeur.domaine.agent.plan.plancoherencecritic.PlanCoherenceCriticOutput;
import storymagine.redacteur.coeur.domaine.orchestrator.common.ScenarioFormatters;
import storymagine.redacteur.coeur.domaine.scenario.Chapter;
import storymagine.redacteur.coeur.domaine.scenario.Scenario;
import storymagine.redacteur.coeur.domaine.story.Story;

/** Activates PlanCoherenceCritic against the current chapter plan in Story. */
public class PlanCoherenceCriticStep {

    private final PlanCoherenceCritic agent;

    public PlanCoherenceCriticStep(PlanCoherenceCritic agent) {
        this.agent = agent;
    }

    public PlanCoherenceCriticOutput run(Scenario scenario, Chapter chapter, Story story) {
        String plan = story.currentChapter().orElseThrow().plan();
        String enrichedPlan = ScenarioFormatters.enrichPlanJson(plan, chapter.sequences());
        return agent.call(new PlanCoherenceCriticInput(
                enrichedPlan,
                chapter.goal(),
                String.join("\n", ScenarioFormatters.planChecks(scenario, chapter)),
                ScenarioFormatters.planConstraints(scenario, chapter),
                ScenarioFormatters.planCharactersText(chapter),
                ScenarioFormatters.focusText(chapter.defaults().focus(), false)
        ));
    }
}
