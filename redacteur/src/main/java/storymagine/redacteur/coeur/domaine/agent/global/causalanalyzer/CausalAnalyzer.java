package storymagine.redacteur.coeur.domaine.agent.global.causalanalyzer;

import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.redacteur.coeur.domaine.agent.Agent;
import storymagine.redacteur.coeur.domaine.agent.commun.ProblemScoreParser;

/**
 * Analyses inter-chapter causal coherence across all chapter plans.
 * Purely informational — run at end of book generation, not during writing.
 * Source: NarrativeCritiqueContext.evalCausal.
 */
public class CausalAnalyzer implements Agent {

    private static final String SYSTEM = """
            Tu es un analyste de cohérence narrative. 
            Tu examines les PLANS de tous les chapitres d'un roman.
            Tu vérifies UNIQUEMENT la cohérence causale entre chapitres :
            chaque événement important a-t-il une cause ?
            
            Signale : événements sans cause, contradictions factuelles entre chapitres,
            conséquences importantes jamais exploitées.
            Format de sortie strict :
            PROBLEME: [description courte d'un problème réel]
            SCORE: N (entier 1-10)

            Exemple 1 — aucun problème :
            PROBLEME: [RIEN]
            SCORE: 10

            Exemple 2 — deux problèmes, score 6 :
            PROBLEME: La trahison de Martin (chapitre 4) n'a aucune cause établie dans les chapitres précédents.
            PROBLEME: La mort de Claire (chapitre 2) n'est jamais exploitée dans la suite.
            SCORE: 6

            En français.
    """;

    private static final String AGENT_NAME = "CausalAnalyzer";

    private final ModelCallPort llm;

    @Override
    public String agentName() { return AGENT_NAME; }

    public CausalAnalyzer(ModelCallPort llm) {
        this.llm = llm;
    }

    public CausalAnalyzerOutput call(CausalAnalyzerInput input) {
        int ctx = llm.contextWindow();
        String plansText = trunc(input.plansText(), ctx * 4 / 3);
        String user = "### Plans des chapitres du roman\n\n" + plansText
            + "\n\nAnalyse la cohérence causale entre les chapitres. Conclus par SCORE: N.";
        String raw = llm.generate(SYSTEM, user, 0.3, LlmCallContext.of(agentName(), agentLabel())).text();
        return new CausalAnalyzerOutput(ProblemScoreParser.parseProblems(raw), ProblemScoreParser.parseScoreInt(raw));
    }

    private static String trunc(String s, int maxChars) {
        if (s == null || s.isBlank()) return "";
        return s.length() <= maxChars ? s : s.substring(0, maxChars) + "…";
    }
}
