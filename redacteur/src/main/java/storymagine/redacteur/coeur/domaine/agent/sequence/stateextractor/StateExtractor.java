package storymagine.redacteur.coeur.domaine.agent.sequence.stateextractor;

import storymagine.commun.coeur.domaine.text.TruncHelper;
import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.LogPort;
import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.redacteur.coeur.domaine.agent.Agent;

/**
 * Extracts structured entity state changes from a just-written sequence.
 * Output format: ETAT: [entity] → [state] / EVENT: [event], or "AUCUN".
 * Called after every sequence write; result accumulated in story memory.
 * Source: StateExtractorContext.extract.
 */
public class StateExtractor implements Agent {

    private static final String SYSTEM = """
        Tu es un tracker d'état narratif. Tu extrais UNIQUEMENT les changements d'état
        des entités physiques après une séquence (personnages, véhicules, objets clés).
        Format strict — une entrée par ligne :
        ETAT: [entité] → [état actuel]
        EVENT: [événement important pour la continuité]
        Si aucun changement notable : réponds exactement AUCUN
        Maximum 5 lignes. En français. Pas de commentaires.""";

    private static final String AGENT_NAME = "SequenceStateExtractor";

    private final ModelCallPort llm;
    private final LogPort       log;

    @Override
    public String agentName() { return AGENT_NAME; }

    public StateExtractor(ModelCallPort llm, LogPort log) {
        this.llm = llm;
        this.log = log;
    }

    public StateExtractorOutput call(StateExtractorInput input) {
        String prevBlock = (input.previousState() != null && !input.previousState().isBlank())
            ? "### État connu\n" + input.previousState() + "\n\n" : "";
        int maxTextChars = llm.contextWindow() * 4 * 60 / 100;
        TruncHelper t = TruncHelper.create();
        String user = prevBlock
            + "### Séquence\n" + t.text(input.sequenceText(), maxTextChars, "sequenceText")
            + "\n\nExtrais les changements d'état.";
        t.logIfTruncated(log, agentName());
        String raw = llm.generate(SYSTEM, user, 0.2, LlmCallContext.of(agentName(), agentLabel()).withThink(thinks())).text();
        return new StateExtractorOutput(raw.trim());
    }
}
