package storymagine.redacteur.coeur.domaine.agent.chapter.goalcritic;

import java.util.List;

import storymagine.commun.coeur.domaine.prompt.PromptBuilder;
import storymagine.commun.coeur.domaine.text.TruncHelper;
import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.LogPort;
import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.redacteur.coeur.domaine.agent.Agent;
import storymagine.redacteur.coeur.domaine.agent.commun.ProblemScoreParser;

/**
 * Evaluates whether a chapter text achieves its declared narrative goal.
 * Separate from critics: this checks narrative function, not quality or coherence.
 * Source: NarrativeGoalContext.evaluateText.
 */
public class ChapterGoalCritic implements Agent {

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
    private final LogPort       log;

    @Override
    public String agentName() { return AGENT_NAME; }

    public ChapterGoalCritic(ModelCallPort llm, LogPort log) {
        this.llm = llm;
        this.log = log;
    }

    public ChapterGoalCriticOutput call(ChapterGoalCriticInput input) {
        int ctx = llm.contextWindow();
        TruncHelper t = TruncHelper.create();
        String user = PromptBuilder.create()
                .section("Objectif narratif de ce chapitre", input.chapterGoal())
                .section("Objectif global du roman (contexte)", t.text(input.bookGoal(), 1600, "bookGoal"))
                .section("Texte à évaluer",                     t.text(input.text(),     ctx * 4 / 2, "text"))
                .raw("Analyse si ce texte atteint l'objectif narratif, puis liste tes PROBLEME:.")
                .build();
        t.logIfTruncated(log, agentName());
        String raw = llm.generate(SYSTEM, user, 0.3, LlmCallContext.of(agentName(), agentLabel()).withThink(thinks())).text();
        List<String> problems = ProblemScoreParser.parseProblems(raw);
        return new ChapterGoalCriticOutput(problems, ProblemScoreParser.scoreFromProblemCount(problems.size()));
    }
}
