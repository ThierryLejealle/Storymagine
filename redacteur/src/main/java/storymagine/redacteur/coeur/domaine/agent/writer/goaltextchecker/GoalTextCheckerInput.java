package storymagine.redacteur.coeur.domaine.agent.writer.goaltextchecker;

/** Input for GoalTextChecker — verifies a chapter text achieves its narrative goal. */
public record GoalTextCheckerInput(String text, String chapterGoal, String bookGoal) {}
