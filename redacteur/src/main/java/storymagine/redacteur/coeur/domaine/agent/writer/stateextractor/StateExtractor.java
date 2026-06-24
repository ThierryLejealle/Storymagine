package storymagine.redacteur.coeur.domaine.agent.writer.stateextractor;

import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.redacteur.coeur.domaine.agent.Agent;

/**
 * Extracts structured entity state changes from a just-written sequence.
 * Output format: ETAT: [entity] → [state] / EVENT: [event], or "AUCUN".
 * Called after every sequence write; result accumulated in story memory.
 * Source: StateExtractorContext.extract.
 */
public class StateExtractor implements Agent {

    private static final int MAX_TEXT_CHARS = 1200;

    private static final String SYSTEM = """
        Tu es un tracker d'état narratif. Tu extrais UNIQUEMENT les changements d'état
        des entités physiques après une séquence (personnages, véhicules, objets clés).
        Format strict — une entrée par ligne :
        ETAT: [entité] → [état actuel]
        EVENT: [événement important pour la continuité]
        Si aucun changement notable : réponds exactement AUCUN
        Maximum 5 lignes. En français. Pas de commentaires.""";

    private static final String AGENT_NAME = "StateExtractor";

    private final ModelCallPort llm;

    @Override
    public String agentName() { return AGENT_NAME; }

    public StateExtractor(ModelCallPort llm) {
        this.llm = llm;
    }

    public StateExtractorOutput call(StateExtractorInput input) {
        String prevBlock = (input.previousState() != null && !input.previousState().isBlank())
            ? "### État connu\n" + input.previousState() + "\n\n" : "";
        String user = prevBlock
            + "### Séquence\n" + trunc(input.sequenceText(), MAX_TEXT_CHARS)
            + "\n\nExtrais les changements d'état.";
        String raw = llm.generate(SYSTEM, user, 0.2, LlmCallContext.of(agentName())).text();
        return new StateExtractorOutput(raw.trim());
    }

    private static String trunc(String s, int maxChars) {
        if (s == null || s.isBlank()) return "";
        return s.length() <= maxChars ? s : s.substring(0, maxChars) + "…";
    }
}
