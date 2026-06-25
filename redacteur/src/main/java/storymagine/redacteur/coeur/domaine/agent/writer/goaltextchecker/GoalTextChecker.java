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

        Echelle de notation :
        10 = objectif pleinement couvert
         9 = excellent — objectif tres bien couvert, quelques legeres imperfections
         8 = tres bien — objectif couvert, quelques sequences a affiner
         7 = bien — objectif couvert mais quelques sequences peuvent mieux le servir
         6 = correct — plusieurs sequences ne servent pas assez l'objectif
         5 = insuffisant — l'objectif est traite de facon trop superficielle
         4 = plusieurs lacunes — l'objectif est secondaire dans le plan
         3 = mauvais — l'objectif n'est qu'en partie adresse
         2 = tres mauvais — l'objectif est absent du plan
         1 = inutilisable — a replanifier integralement

        FORMAT STRICT :
        PROBLEME: une ligne par probleme, ou [RIEN] si aucun probleme.
        SCORE: la note que tu as determinee (entier 1-10)
        Rien d'autre : ni texte avant ni texte apres ces lignes.

        Exemple 1 — deux problemes, note 7 :
        PROBLEME: Le personnage n'exprime pas de doute alors que l'objectif l'exige.
        PROBLEME: La resolution arrive trop tot, sans tension prealable.
        SCORE: 7

        Exemple 2 — aucun probleme, note 10 :
        PROBLEME: [RIEN]
        SCORE: 10

        En francais.""";

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
