package storymagine.redacteur.coeur.domaine.agent.writer.stylecorrector;

/** Input for StyleCorrector. */
public record StyleCorrectorInput(
    String text,
    String styleGuide,
    String qualityCriteria,
    String writingExample
) {}
