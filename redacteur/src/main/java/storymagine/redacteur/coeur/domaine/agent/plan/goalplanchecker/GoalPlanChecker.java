package storymagine.redacteur.coeur.domaine.agent.plan.goalplanchecker;

import java.util.List;

import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.redacteur.coeur.domaine.agent.Agent;
import storymagine.redacteur.coeur.domaine.agent.commun.ProblemScoreParser;

/**
 * Evaluates whether a chapter plan fulfils its declared narrative goal ("but du chapitre").
 * Separate from critics: this checks narrative function, not quality or coherence.
 * Source: NarrativeGoalContext.evaluatePlan.
 */
public class GoalPlanChecker implements Agent {

    private static final String SYSTEM =
    """
Tu évalues si un PLAN DE CHAPITRE remplit son objectif narratif spécifique.
Ne juge pas la qualité littéraire, ni la cohérence globale du roman.
Uniquement : le plan avance-t-il clairement et concrètement vers l'objectif narratif ?

Procède dans cet ordre :
1. Analyse le plan entier.
2. Note tous les défauts et axes d'amélioration.
3. Liste en sortie les défauts trouvés.

Écris UNIQUEMENT le FORMAT STRICT ci-dessous — ne produis pas les étapes intermédiaires 1 et 2 dans ta réponse.
FORMAT STRICT :
PROBLEME: une ligne par problème, ou [RIEN] si aucun problème.
Rien d'autre : ni texte avant ni texte apres.

Exemple 1 - deux problèmes :
PROBLEME: "L'ours n'a pas de chemise"
PROBLEME: "Le lapin est vert"

Exemple 2 - aucun probleme trouve :
PROBLEME: [RIEN]

En français.
""";

    private static final String AGENT_NAME = "PlanGoalChecker";

    private final ModelCallPort llm;

    @Override
    public String agentName() { return AGENT_NAME; }

    public GoalPlanChecker(ModelCallPort llm) {
        this.llm = llm;
    }

    public GoalPlanCheckerOutput call(GoalPlanCheckerInput input) {
        int ctx = llm.contextWindow();
        String user = section("Objectif narratif de ce chapitre", input.chapterGoal())
                + section("Objectif global du roman (contexte)", trunc(input.bookGoal(), 1600))
                + section("Plan à évaluer",                      trunc(input.plan(),     ctx * 4 / 2))
                + "\n\nAnalyse si ce plan remplit l'objectif narratif, puis liste tes PROBLEME:.";
        String raw = llm.generate(SYSTEM, user, 0.3, LlmCallContext.of(agentName(), agentLabel())).text();
        List<String> problems = ProblemScoreParser.parseProblems(raw);
        return new GoalPlanCheckerOutput(problems, ProblemScoreParser.scoreFromProblemCount(problems.size()));
    }

    private static String section(String title, String content) {
        if (content == null || content.isBlank()) return "";
        return "\n\n### " + title + "\n" + content;
    }

    private static String trunc(String s, int maxChars) {
        if (s == null || s.isBlank()) return "";
        return s.length() <= maxChars ? s : s.substring(0, maxChars) + "…";
    }
}
