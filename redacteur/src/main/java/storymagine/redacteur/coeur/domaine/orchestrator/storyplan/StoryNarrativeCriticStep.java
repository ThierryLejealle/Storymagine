package storymagine.redacteur.coeur.domaine.orchestrator.storyplan;

import storymagine.redacteur.coeur.domaine.agent.storyplan.storynarrativecritic.StoryNarrativeCritic;
import storymagine.redacteur.coeur.domaine.agent.storyplan.storynarrativecritic.StoryNarrativeCriticInput;
import storymagine.redacteur.coeur.domaine.agent.storyplan.storynarrativecritic.StoryNarrativeCriticOutput;

/** Activates StoryNarrativeCritic against the book goal and every chapter's brief + generated plan. */
public class StoryNarrativeCriticStep {

    private final StoryNarrativeCritic agent;

    public StoryNarrativeCriticStep(StoryNarrativeCritic agent) {
        this.agent = agent;
    }

    public StoryNarrativeCriticOutput run(String bookGoal, String chaptersBlock) {
        return agent.call(new StoryNarrativeCriticInput(bookGoal, chaptersBlock));
    }
}
