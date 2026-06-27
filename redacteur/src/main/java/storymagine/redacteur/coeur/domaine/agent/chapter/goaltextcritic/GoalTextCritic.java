package storymagine.redacteur.coeur.domaine.agent.chapter.goaltextcritic;

import java.util.List;

import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.redacteur.coeur.domaine.agent.Agent;
import storymagine.redacteur.coeur.domaine.agent.commun.ProblemScoreParser;

/**
 * Evaluates whether a chapter text achieves its declared narrative goal.
 * Separate from critics: this checks narrative function, not quality or coherence.
 * Source: NarrativeGoalContext.evaluateText.
 */
public class GoalTextCritic implements Agent {

    private static final String SYSTEM = """
        Tu évalues si un TEXTE DE CHAPITRE remplit son objectif narratif spécifique.
        Ne juge pas la qualité littéraire ni la cohérence avec l'ensemble du roman.
        Uniquement : le texte produit-il l'effet narratif ou émotionnel requis par l'objectif ?

        FORMAT STRICT :
        PROBLEME: une ligne par probleme, ou [RIEN] si aucun probleme.
        Rien d'autre : ni texte avant ni texte apres.

        Exemple 1 — deux problemes :
        PROBLEME: Le personnage n'exprime pas de doute alors que l'objectif l'exige.
        PROBLEME: La resolution arrive trop tot, sans tension prealable.

        Exemple 2 — aucun probleme :
        PROBLEME: [RIEN]

        En francais.""";

    private static final String AGENT_NAME = "ChapterGoalCritic";

    private final ModelCallPort llm;

    @Override
    public String agentName() { return AGENT_NAME; }

    public GoalTextCritic(ModelCallPort llm) {
        this.llm = llm;
    }

    public GoalTextCriticOutput call(GoalTextCriticInput input) {
        int ctx = llm.contextWindow();
        String user = section("Objectif narratif de ce chapitre", input.chapterGoal())
                + section("Objectif global du roman (contexte)", trunc(input.bookGoal(), 1600))
                + section("Texte à évaluer",                     trunc(input.text(),     ctx * 4 / 2))
                + "\n\nAnalyse si ce texte atteint l'objectif narratif, puis liste tes PROBLEME:.";
        String raw = llm.generate(SYSTEM, user, 0.3, LlmCallContext.of(agentName(), agentLabel())).text();
        List<String> problems = ProblemScoreParser.parseProblems(raw);
        return new GoalTextCriticOutput(problems, ProblemScoreParser.scoreFromProblemCount(problems.size()));
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
