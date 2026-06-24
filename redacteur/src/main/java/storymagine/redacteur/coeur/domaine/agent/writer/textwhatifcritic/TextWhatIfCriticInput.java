package storymagine.redacteur.coeur.domaine.agent.writer.textwhatifcritic;

/** Input for TextWhatIfCritic — evaluates physical/causal plausibility of a what-if chapter. */
public record TextWhatIfCriticInput(String text, String constraints) {}
