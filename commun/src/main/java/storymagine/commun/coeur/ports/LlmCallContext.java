package storymagine.commun.coeur.ports;

/**
 * Metadata attached to an LLM generate() call — agent identity and optional free-text note.
 * Used for tracing (llm_calls/ files) and observability, not for prompt content.
 */
public record LlmCallContext(String agentName, String note) {

    public static LlmCallContext of(String agentName) {
        return new LlmCallContext(agentName, "");
    }

    public static LlmCallContext of(String agentName, String note) {
        return new LlmCallContext(agentName, note == null ? "" : note);
    }
}
