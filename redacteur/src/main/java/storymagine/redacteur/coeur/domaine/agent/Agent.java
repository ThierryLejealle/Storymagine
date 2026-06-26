package storymagine.redacteur.coeur.domaine.agent;

/** Marker interface for all LLM agents — provides a canonical agent name and a log label. */
public interface Agent {
    String agentName();

    /** Returns "category/AgentName" where category is the parent package under /agent (plan, writer, global…). */
    default String agentLabel() {
        String pkg = getClass().getPackageName();
        String[] parts = pkg.split("\\.");
        for (int i = 0; i < parts.length - 1; i++) {
            if ("agent".equals(parts[i])) return parts[i + 1] + "/" + agentName();
        }
        return agentName();
    }
}
