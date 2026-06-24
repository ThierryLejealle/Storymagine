package storymagine.redacteur.coeur.domaine.agent.global.chapterstylechecker;

/** Input for ChapterStyleChecker — full chapter text + style references. */
public record ChapterStyleCheckerInput(
    String text,
    String styleGuide,
    String qualityCriteria,
    String writingExample
) {}
