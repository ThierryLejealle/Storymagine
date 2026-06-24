package storymagine.commun.coeur.ports;

/**
 * Port d'appel à un LLM — contrat utilisé par les agents.
 */
public interface ModelCallPort {

    /**
     * @param systemPrompt  instructions de rôle / contexte narratif
     * @param userPrompt    consigne de l'agent
     * @param temperature   0..1 — créativité (0.8 écriture, 0.3 critique)
     * @param ctx           identité de l'appelant (agent, note) — utilisé pour la traçabilité
     * @return résultat généré par le LLM
     */
    LlmResult generate(String systemPrompt, String userPrompt, double temperature, LlmCallContext ctx);

    /**
     * Surcharge sans contexte — réservée aux tests et appels internes (context = Unknown).
     */
    default LlmResult generate(String systemPrompt, String userPrompt, double temperature) {
        return generate(systemPrompt, userPrompt, temperature, LlmCallContext.of("Unknown"));
    }

    /**
     * Nombre de tokens que le modèle peut traiter en un seul appel (ex. 8192, 32768).
     * Utilisé par les agents pour dimensionner leurs slots de contenu.
     */
    int contextWindow();
}
