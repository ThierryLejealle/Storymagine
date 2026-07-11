package storymagine.redacteur.coeur.domaine.agent.chapter.dreamcritic;

/** Input for ChapterDreamCritic — evaluates oneiric quality of a dream chapter. */
public record ChapterDreamCriticInput(String text, String bookGoal, String realismLevel) {}
