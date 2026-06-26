package storymagine.redacteur.coeur.domaine.agent.writer.goaltextcritic;

/** Input for GoalTextCritic — verifies a chapter text achieves its narrative goal. */
public record GoalTextCriticInput(String text, String chapterGoal, String bookGoal) {}
