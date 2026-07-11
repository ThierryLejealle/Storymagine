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

    /**
     * Whether this agent's LLM calls should request active reasoning (thinking). True by default —
     * override to false only for closed/mechanical tasks where reasoning doesn't improve judgment
     * (see evols/2026-07-09-2319-derive-correcteurs-diagnostic-et-pistes.md, §6.4).
     */
    default boolean thinks() { return true; }
}
