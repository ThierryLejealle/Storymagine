package storymagine.commun.coeur.ports;

/**
 * Metadata attached to an LLM generate() call — agent identity and optional free-text note.
 * agentName: short name used for file naming; agentLabel: "category/Name" used in console logs.
 */
public record LlmCallContext(String agentName, String agentLabel, String note) {

    public static LlmCallContext of(String agentName) {
        return new LlmCallContext(agentName, agentName, "");
    }

    public static LlmCallContext of(String agentName, String agentLabel) {
        return new LlmCallContext(agentName, agentLabel, "");
    }
}
