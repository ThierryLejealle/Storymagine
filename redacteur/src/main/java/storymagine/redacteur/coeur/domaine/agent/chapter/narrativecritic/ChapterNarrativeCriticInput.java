package storymagine.redacteur.coeur.domaine.agent.chapter.narrativecritic;

/** Input for ChapterNarrativeCritic — evaluates narrative arc progression of a chapter text. */
public record ChapterNarrativeCriticInput(String text, String chapterDescription, String chapterGoal, String bookGoal) {}
