package storymagine.redacteur.coeur.domaine.agent.storyplan.storynarrativecritic;

/**
 * Input for StoryNarrativeCritic — book goal and, per chapter, the author's brief
 * (description + goal) followed by the generated plan, in that order.
 */
public record StoryNarrativeCriticInput(String bookGoal, String chaptersBlock) {}
