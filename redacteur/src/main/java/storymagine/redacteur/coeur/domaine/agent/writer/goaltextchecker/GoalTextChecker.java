package storymagine.redacteur.coeur.domaine.agent.writer.goaltextchecker;

import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.redacteur.coeur.domaine.agent.Agent;
import storymagine.redacteur.coeur.domaine.agent.commun.ProblemScoreParser;

/**
 * Evaluates whether a chapter text achieves its declared narrative goal.
 * Separate from critics: this checks narrative function, not quality or coherence.
 * Source: NarrativeGoalContext.evaluateText.
 */
public class GoalTextChecker implements Agent {

    private static final String SYSTEM = """
        Tu évalues si un TEXTE DE CHAPITRE remplit son objectif narratif spécifique.
        Ne juge pas la qualité littéraire ni la cohérence avec l'ensemble du roman.
        Uniquement : le texte produit-il l'effet narratif ou émotionnel requis par l'objectif ?

        Échelle de notation :
        10 = objectif pleinement atteint
         9 = excellent — objectif bien atteint, quelques légères imperfections
         8 = bon — bien atteint, des moments pourraient mieux servir l'objectif
         7 = atteint mais insuffisamment — des passages dérivent de l'objectif
         6 = partiellement atteint — l'effet narratif reste flou sur une partie du texte
         5 = mal atteint — le texte produit peu ou pas l'effet requis
         3 = à réécrire : le texte rate l'objectif narratif

        Format obligatoire :
        PROBLEME: [défaut ou axe d'amélioration]
        (une ligne PROBLEME: par défaut réellement constaté — ne pas en inventer)
        Si score = 10 : aucune ligne PROBLEME:
        SCORE: N  (entier 0-10)
        En français.""";

    private static final String AGENT_NAME = "GoalTextChecker";

    private final ModelCallPort llm;

    @Override
    public String agentName() { return AGENT_NAME; }

    public GoalTextChecker(ModelCallPort llm) {
        this.llm = llm;
    }

    public GoalTextCheckerOutput call(GoalTextCheckerInput input) {
        int ctx = llm.contextWindow();
        String user = section("Objectif narratif de ce chapitre", input.chapterGoal())
                + section("Objectif global du roman (contexte)", trunc(input.bookGoal(), 1600))
                + section("Texte à évaluer",                     trunc(input.text(),     ctx * 4 / 2))
                + "\n\nAnalyse si ce texte atteint l'objectif narratif, puis conclus avec tes PROBLEME: et SCORE: N.";
        String raw = llm.generate(SYSTEM, user, 0.3, LlmCallContext.of(agentName())).text();
        return new GoalTextCheckerOutput(ProblemScoreParser.parseProblems(raw), ProblemScoreParser.parseScore(raw));
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
