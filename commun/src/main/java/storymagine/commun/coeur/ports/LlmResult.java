package storymagine.commun.coeur.ports;

/**
 * Résultat d'un appel LLM. thinking est le texte de réflexion renvoyé par Ollama quand think:true
 * a été demandé (LlmCallContext.withThink) — vide sinon.
 */
public record LlmResult(String text, int promptTokens, int responseTokens, long evalDurationNs, String thinking) {

    public static LlmResult of(String text) {
        return new LlmResult(text, 0, 0, 0, "");
    }

    public double tokensPerSecond() {
        if (evalDurationNs <= 0 || responseTokens <= 0) return 0;
        return responseTokens / (evalDurationNs / 1_000_000_000.0);
    }
}
