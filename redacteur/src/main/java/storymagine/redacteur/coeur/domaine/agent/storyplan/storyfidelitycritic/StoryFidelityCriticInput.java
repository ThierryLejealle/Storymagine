package storymagine.redacteur.coeur.domaine.agent.storyplan.storyfidelitycritic;

/**
 * Input for StoryFidelityCritic — book goal and, per chapter, the author's brief
 * (description + goal) followed by the generated plan, in that order.
 */
public record StoryFidelityCriticInput(String bookGoal, String chaptersBlock) {}
