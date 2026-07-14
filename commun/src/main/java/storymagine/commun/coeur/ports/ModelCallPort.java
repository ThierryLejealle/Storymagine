package storymagine.commun.coeur.ports;

import java.util.List;

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
     * Comme generate(), avec des séquences qui interrompent la génération dès qu'elles apparaissent
     * — ex. empêcher un petit modèle de continuer et d'écrire le tour suivant à la place de
     * l'interlocuteur. Raccourci pour generate(..., GenerationOptions.stopSequences(...)).
     */
    default LlmResult generate(String systemPrompt, String userPrompt, double temperature, LlmCallContext ctx,
                                List<String> stopSequences) {
        return generate(systemPrompt, userPrompt, temperature, ctx, GenerationOptions.stopSequences(stopSequences));
    }

    /**
     * Comme generate(), avec des réglages ponctuels pour cet appel (voir GenerationOptions).
     * Défaut : délègue à generate() sans y toucher — les adaptateurs qui ne surchargent pas cette
     * méthode gardent leur comportement actuel.
     */
    default LlmResult generate(String systemPrompt, String userPrompt, double temperature, LlmCallContext ctx,
                                GenerationOptions options) {
        return generate(systemPrompt, userPrompt, temperature, ctx);
    }

    /**
     * Nombre de tokens que le modèle peut traiter en un seul appel (ex. 8192, 32768).
     * Utilisé par les agents pour dimensionner leurs slots de contenu.
     */
    int contextWindow();
}
