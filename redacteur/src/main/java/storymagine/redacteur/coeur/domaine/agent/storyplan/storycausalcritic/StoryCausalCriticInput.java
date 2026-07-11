package storymagine.redacteur.coeur.domaine.agent.storyplan.storycausalcritic;

/**
 * Input for StoryCausalCritic — book goal and, per chapter, the author's brief
 * (description + goal) followed by the generated plan, in that order.
 */
public record StoryCausalCriticInput(String bookGoal, String chaptersBlock) {}
