package storymagine.redacteur.coeur.domaine.agent.chapter.whatifcritic;

import storymagine.commun.coeur.domaine.prompt.PromptBuilder;
import storymagine.commun.coeur.domaine.text.TruncHelper;
import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.LogPort;
import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.redacteur.coeur.domaine.agent.Agent;
import storymagine.redacteur.coeur.domaine.agent.commun.ProblemScoreParser;

import java.util.List;

/**
 * Evaluates physical and causal plausibility of a what-if chapter.
 * Does not check coherence with main story — divergence from main story is intentional.
 * Source: CriticContext.evalWhatIfPlausibility.
 */
public class ChapterWhatIfCritic implements Agent {

    private static final String SYSTEM = """
            Tu évalues un SCÉNARIO ALTERNATIF (what-if), pas la trame réelle du roman.
            Critères UNIQUEMENT : plausibilité physique et causale de l'hypothèse
            (les conséquences découlent-elles logiquement de la prémisse ?),
            cohérence des lois physiques et de la réalité matérielle dans ce monde alternatif,
            crédibilité psychologique des personnages face à la nouvelle situation.
            Ignore toute incohérence avec les événements réels du roman —
            la divergence par rapport à la trame principale est intentionnelle et non un problème.

            Échelle de notation :
            10 = hypothèse parfaitement plausible
             9 = excellent
             8 = bon
             7 = globalement plausible mais une incohérence causale notable
             6 = hypothèse acceptable mais conséquences manquent de rigueur causale
             5 = incohérences causales qui brisent la suspension d'incrédulité
             4 = incohérences sérieuses multiples
             3 = à réécrire
             2 = prémisse invalidée par ses propres conséquences
             1 = inutilisable

            Format de sortie strict :
            PROBLEME: [défaut ou axe d'amélioration]
            SCORE: N  (entier 1-10)
            En français.""";

    private static final String AGENT_NAME = "ChapterWhatIfCritic";

    private final ModelCallPort llm;
    private final LogPort       log;

    @Override
    public String agentName() { return AGENT_NAME; }

    public ChapterWhatIfCritic(ModelCallPort llm, LogPort log) {
        this.llm = llm;
        this.log = log;
    }

    public ChapterWhatIfCriticOutput call(ChapterWhatIfCriticInput input) {
        int ctx = llm.contextWindow();
        TruncHelper t = TruncHelper.create();
        String user = PromptBuilder.create()
            .section("Texte du scénario alternatif", t.text(input.text(), ctx * 4 * 55 / 100, "text"))
            .section("Points à vérifier (monde/physique)", "Vérifie que chacun des points suivants est respecté :\n" + t.list(input.checks(), ctx * 4 / 8, "checks"))
            .raw("Évalue la plausibilité physique et causale. Conclus par SCORE: N.")
            .build();
        t.logIfTruncated(log, agentName());
        String raw = llm.generate(SYSTEM, user, 0.3, LlmCallContext.of(agentName(), agentLabel()).withThink(thinks())).text();
        List<String> problems = ProblemScoreParser.parseProblems(raw);
        double score = ProblemScoreParser.parseScore(raw);
        return new ChapterWhatIfCriticOutput(problems, score);
    }
}
