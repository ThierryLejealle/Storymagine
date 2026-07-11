package storymagine.redacteur.coeur.domaine.orchestrator.plan;

import storymagine.redacteur.coeur.domaine.agent.plan.coherencecritic.PlanCoherenceCritic;
import storymagine.redacteur.coeur.domaine.agent.plan.coherencecritic.PlanCoherenceCriticInput;
import storymagine.redacteur.coeur.domaine.agent.plan.coherencecritic.PlanCoherenceCriticOutput;
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
        return agent.call(new PlanCoherenceCriticInput(
                plan,
                ScenarioFormatters.sequenceDirectivesBlock(chapter.sequences()),
                scenario.config().language()
        ));
    }
}
