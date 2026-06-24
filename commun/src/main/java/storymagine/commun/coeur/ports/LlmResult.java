package storymagine.commun.coeur.ports;

/**
 * Résultat d'un appel LLM.
 */
public record LlmResult(String text, int promptTokens, int responseTokens, long evalDurationNs) {

    public static LlmResult of(String text) {
        return new LlmResult(text, 0, 0, 0);
    }

    public double tokensPerSecond() {
        if (evalDurationNs <= 0 || responseTokens <= 0) return 0;
        return responseTokens / (evalDurationNs / 1_000_000_000.0);
    }
}
