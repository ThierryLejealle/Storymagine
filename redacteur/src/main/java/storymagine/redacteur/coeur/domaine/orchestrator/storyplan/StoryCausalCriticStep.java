package storymagine.redacteur.coeur.domaine.orchestrator.storyplan;

import storymagine.redacteur.coeur.domaine.agent.storyplan.storycausalcritic.StoryCausalCritic;
import storymagine.redacteur.coeur.domaine.agent.storyplan.storycausalcritic.StoryCausalCriticInput;
import storymagine.redacteur.coeur.domaine.agent.storyplan.storycausalcritic.StoryCausalCriticOutput;

/** Activates StoryCausalCritic against the book goal and every chapter's brief + generated plan. */
public class StoryCausalCriticStep {

    private final StoryCausalCritic agent;

    public StoryCausalCriticStep(StoryCausalCritic agent) {
        this.agent = agent;
    }

    public StoryCausalCriticOutput run(String bookGoal, String chaptersBlock) {
        return agent.call(new StoryCausalCriticInput(bookGoal, chaptersBlock));
    }
}
