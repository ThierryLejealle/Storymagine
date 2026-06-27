package storymagine.redacteur.coeur.domaine.agent.sequence.stylecorrector;

/** Input for StyleCorrector. */
public record StyleCorrectorInput(
    String text,
    String styleGuide,
    String qualityCriteria,
    String writingExample
) {}
