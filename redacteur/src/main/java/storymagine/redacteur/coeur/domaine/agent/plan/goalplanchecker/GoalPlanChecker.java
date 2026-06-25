package storymagine.redacteur.coeur.domaine.agent.plan.goalplanchecker;

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

Échelle de notation :
10 = objectif pleinement couvert
 9 = excellent — objectif très bien couvert, quelques légères imperfections
 8 = très bien — objectif couvert, quelques séquences à affiner
 7 = bien — objectif couvert mais quelques séquences peuvent mieux le servir
 6 = correct — plusieurs séquences ne servent pas assez l'objectif
 5 = insuffisant — l'objectif est traité de façon trop superficielle
 4 = plusieurs lacunes — l'objectif est secondaire dans le plan
 3 = mauvais — l'objectif n'est qu'en partie adressé
 2 = très mauvais — l'objectif est absent du plan
 1 = inutilisable — à replanifier intégralement

Procède dans cet ordre :
1. Analyse le plan entier.
2. Note tous les défauts et axes d'amélioration.
3. Détermine la note en fonction de la qualité globale.
4. Liste en sortie défauts et axes d'amélioration trouvés.

FORMAT STRICT :
PROBLEME: une ligne par problème, ou [RIEN] si aucun problème.
SCORE: la note que tu as déterminée  (entier 1-10)
Rien d'autre : ni texte avant ni texte apres ces trois lignes.

Exemple 1 - deux problèmes et une note de 8 :
PROBLEME: "L'ours n'a pas de chemise"
PROBLEME: "Le lapin est vert"
SCORE: 8

Exemple 2 - aucun probleme trouve et note de 10 :
PROBLEME: [RIEN]
SCORE: 10

En français.
""";

    private static final String AGENT_NAME = "GoalPlanChecker";

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
                + "\n\nAnalyse si ce plan remplit l'objectif narratif, puis conclus avec tes PROBLEME: et SCORE: N.";
        String raw = llm.generate(SYSTEM, user, 0.3, LlmCallContext.of(agentName())).text();
        return new GoalPlanCheckerOutput(ProblemScoreParser.parseProblems(raw), ProblemScoreParser.parseScore(raw));
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
