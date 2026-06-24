package storymagine.redacteur.coeur.domaine.agent.writer.textcoherencecritic;

/** Input for TextCoherenceCritic — checks factual coherence of a chapter text. */
public record TextCoherenceCriticInput(
    String text,
    String checks,
    String constraints,
    String focusText
) {}
