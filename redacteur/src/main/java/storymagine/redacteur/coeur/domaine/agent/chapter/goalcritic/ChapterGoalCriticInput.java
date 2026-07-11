package storymagine.redacteur.coeur.domaine.agent.chapter.goalcritic;

/** Input for ChapterGoalCritic — verifies a chapter text achieves its narrative goal. */
public record ChapterGoalCriticInput(String text, String chapterGoal, String bookGoal) {}
