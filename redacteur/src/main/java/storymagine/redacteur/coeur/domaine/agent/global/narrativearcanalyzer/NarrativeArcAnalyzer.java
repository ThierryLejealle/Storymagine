package storymagine.redacteur.coeur.domaine.agent.global.narrativearcanalyzer;

import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.redacteur.coeur.domaine.agent.Agent;
import storymagine.redacteur.coeur.domaine.agent.commun.ProblemScoreParser;

/**
 * Analyses character arcs across all chapter plans of the book.
 * Purely informational — run at end of book generation, not during writing.
 * Source: NarrativeCritiqueContext.evalArcs.
 */
public class NarrativeArcAnalyzer implements Agent {

    private static final String SYSTEM =
        "Tu es un analyste de structure narrative. Tu examines les PLANS de tous les chapitres d'un roman.\n"
        + "Tu vérifies UNIQUEMENT les arcs narratifs des personnages : introduction, évolution, résolution.\n"
        + "Signale : arcs ouverts sans résolution, personnages qui disparaissent sans explication,\n"
        + "arcs redondants entre personnages, tournants promis et jamais tenus.\n"
        + "Si rien à signaler, écris SCORE: 10 sans PROBLEME.\n\n"
        + "Format de sortie strict :\n"
        + "PROBLEME: [description courte d'un problème réel]\n"
        + "SCORE: N  (entier 0-10)\n"
        + "En français.";

    private static final String AGENT_NAME = "NarrativeArcAnalyzer";

    private final ModelCallPort llm;

    @Override
    public String agentName() { return AGENT_NAME; }

    public NarrativeArcAnalyzer(ModelCallPort llm) {
        this.llm = llm;
    }

    public NarrativeArcAnalyzerOutput call(NarrativeArcAnalyzerInput input) {
        int ctx = llm.contextWindow();
        String plansText = trunc(input.plansText(), ctx * 4 / 3);
        String user = "### Plans des chapitres du roman\n\n" + plansText
            + "\n\nAnalyse les arcs narratifs des personnages. Conclus par SCORE: N.";
        String raw = llm.generate(SYSTEM, user, 0.3, LlmCallContext.of(agentName())).text();
        return new NarrativeArcAnalyzerOutput(ProblemScoreParser.parseProblems(raw), ProblemScoreParser.parseScoreInt(raw));
    }

    private static String trunc(String s, int maxChars) {
        if (s == null || s.isBlank()) return "";
        return s.length() <= maxChars ? s : s.substring(0, maxChars) + "…";
    }
}
