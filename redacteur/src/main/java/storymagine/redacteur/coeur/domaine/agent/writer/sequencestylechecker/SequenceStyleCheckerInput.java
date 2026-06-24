package storymagine.redacteur.coeur.domaine.agent.writer.sequencestylechecker;

/** Input for SequenceStyleChecker. */
public record SequenceStyleCheckerInput(
    String text,
    String styleGuide,
    String qualityCriteria,
    String writingExample
) {}
