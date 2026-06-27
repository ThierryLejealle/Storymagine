package storymagine.redacteur.coeur.domaine.agent.temp.chapterstylechecker;

/** Input for ChapterStyleChecker â€” full chapter text + style references. */
public record ChapterStyleCheckerInput(
    String text,
    String styleGuide,
    String qualityCriteria,
    String writingExample
) {}
