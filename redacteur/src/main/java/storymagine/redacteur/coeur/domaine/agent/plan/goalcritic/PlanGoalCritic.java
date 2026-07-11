package storymagine.redacteur.coeur.domaine.agent.plan.goalcritic;

import java.util.List;

import storymagine.commun.coeur.domaine.prompt.PromptBuilder;
import storymagine.commun.coeur.domaine.text.TruncHelper;
import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.LogPort;
import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.redacteur.coeur.domaine.agent.Agent;
import storymagine.redacteur.coeur.domaine.agent.commun.ProblemScoreParser;

/**
 * Evaluates whether a chapter plan fulfils its declared narrative goal ("but du chapitre").
 * Judges narrative function, not prose quality or coherence — those are other critics' job.
 * Source: NarrativeGoalContext.evaluatePlan.
 */
public class PlanGoalCritic implements Agent {

    private static final String SYSTEM =
    """
Tu évalues si un PLAN DE CHAPITRE remplit son objectif narratif spécifique.
Ne juge pas la qualité littéraire, ni la cohérence globale du roman — c'est le rôle d'un autre agent, jamais le tien.
Uniquement : le plan avance-t-il clairement et concrètement vers la consigne et l'objectif narratif de ce chapitre ?
Si la consigne ou l'objectif de ce chapitre sont fournis, tout element du plan qui en decoule directement n'est pas un defaut — ne le signale pas.

Procède dans cet ordre :
1. Analyse le plan entier.
2. Note tous les défauts et axes d'amélioration PAR RAPPORT A LA CONSIGNE ET A L'OBJECTIF DE CE CHAPITRE (jamais la cohérence générale, jamais la qualité littéraire).
3. Liste en sortie les défauts trouvés.

Écris UNIQUEMENT le FORMAT STRICT ci-dessous — ne produis pas les étapes intermédiaires 1 et 2 dans ta réponse.
FORMAT STRICT :
PROBLEME: une ligne par problème, ou [RIEN] si aucun problème.
Rien d'autre : ni texte avant ni texte apres.

Exemple 1 - deux problèmes (objectif du chapitre : "installer la méfiance de Pierre envers son capitaine") :
PROBLEME: "Le plan ne fait jamais interagir Pierre et le capitaine — la méfiance ne peut pas s'installer si rien ne la déclenche"
PROBLEME: "La S3 montre au contraire Pierre complimentant chaleureusement le capitaine, ce qui va à l'encontre de l'objectif"

Exemple 2 - aucun probleme trouve :
PROBLEME: [RIEN]

En français.
""";

    private static final String AGENT_NAME = "PlanGoalCritic";

    private final ModelCallPort llm;
    private final LogPort       log;

    @Override
    public String agentName() { return AGENT_NAME; }

    public PlanGoalCritic(ModelCallPort llm, LogPort log) {
        this.llm = llm;
        this.log = log;
    }

    public PlanGoalCriticOutput call(PlanGoalCriticInput input) {
        int ctx = llm.contextWindow();
        TruncHelper t = TruncHelper.create();
        String user = PromptBuilder.create()
                .section("Consigne de l'auteur (ce chapitre)",  t.text(input.chapterDescription(), ctx * 4 / 12, "chapterDescription"))
                .section("Objectif narratif de ce chapitre",    input.chapterGoal())
                .section("Objectif global du roman (contexte)", t.text(input.bookGoal(), 1600, "bookGoal"))
                .section("Plan à évaluer",                      t.text(input.plan(),     ctx * 4 / 2, "plan"))
                .raw("Analyse si ce plan avance vers la consigne et l'objectif narratif de ce chapitre, puis liste tes PROBLEME:.")
                .build();
        t.logIfTruncated(log, agentName());
        String raw = llm.generate(SYSTEM, user, 0.3, LlmCallContext.of(agentName(), agentLabel()).withThink(thinks())).text();
        List<String> problems = ProblemScoreParser.parseProblems(raw);
        return new PlanGoalCriticOutput(problems, ProblemScoreParser.scoreFromProblemCount(problems.size()));
    }
}
