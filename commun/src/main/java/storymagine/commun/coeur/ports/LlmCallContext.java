package storymagine.commun.coeur.ports;

/**
 * Metadata attached to an LLM generate() call — agent identity and optional free-text note.
 * agentName: short name used for file naming; agentLabel: "category/Name" used in console logs.
 * think: préférence de réflexion pour cet appel — null = pas de préférence (hérite du réglage
 * par défaut de l'adaptateur), TRUE/FALSE = demande explicite pour cet agent.
 */
public record LlmCallContext(String agentName, String agentLabel, String note, Boolean think) {

    public static LlmCallContext of(String agentName) {
        return new LlmCallContext(agentName, agentName, "", null);
    }

    public static LlmCallContext of(String agentName, String agentLabel) {
        return new LlmCallContext(agentName, agentLabel, "", null);
    }

    /** Copie ce contexte avec une préférence de réflexion explicite pour cet appel. */
    public LlmCallContext withThink(Boolean think) {
        return new LlmCallContext(agentName, agentLabel, note, think);
    }
}
