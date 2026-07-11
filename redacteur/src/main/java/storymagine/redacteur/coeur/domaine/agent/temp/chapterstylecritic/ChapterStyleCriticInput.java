package storymagine.redacteur.coeur.domaine.agent.temp.chapterstylecritic;

/** Input for ChapterStyleCritic â€” full chapter text + style references. */
public record ChapterStyleCriticInput(
    String text,
    String styleGuide,
    String qualityCriteria,
    String writingExample
) {}
