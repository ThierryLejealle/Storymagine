package storymagine.redacteur.coeur.domaine.orchestrator.storyplan;

import storymagine.redacteur.coeur.domaine.agent.storyplan.storyfidelitycritic.StoryFidelityCritic;
import storymagine.redacteur.coeur.domaine.agent.storyplan.storyfidelitycritic.StoryFidelityCriticInput;
import storymagine.redacteur.coeur.domaine.agent.storyplan.storyfidelitycritic.StoryFidelityCriticOutput;

/** Activates StoryFidelityCritic against the book goal and every chapter's brief + generated plan. */
public class StoryFidelityCriticStep {

    private final StoryFidelityCritic agent;

    public StoryFidelityCriticStep(StoryFidelityCritic agent) {
        this.agent = agent;
    }

    public StoryFidelityCriticOutput run(String bookGoal, String chaptersBlock) {
        return agent.call(new StoryFidelityCriticInput(bookGoal, chaptersBlock));
    }
}
