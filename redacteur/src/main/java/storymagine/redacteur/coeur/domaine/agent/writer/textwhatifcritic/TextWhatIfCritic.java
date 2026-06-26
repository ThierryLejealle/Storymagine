package storymagine.redacteur.coeur.domaine.agent.writer.textwhatifcritic;

import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.redacteur.coeur.domaine.agent.Agent;
import storymagine.redacteur.coeur.domaine.agent.commun.ProblemScoreParser;

import java.util.List;

/**
 * Evaluates physical and causal plausibility of a what-if chapter.
 * Does not check coherence with main story — divergence from main story is intentional.
 * Source: CriticContext.evalWhatIfPlausibility.
 */
public class TextWhatIfCritic implements Agent {

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

    private static final String AGENT_NAME = "TextWhatIfCritic";

    private final ModelCallPort llm;

    @Override
    public String agentName() { return AGENT_NAME; }

    public TextWhatIfCritic(ModelCallPort llm) {
        this.llm = llm;
    }

    public TextWhatIfCriticOutput call(TextWhatIfCriticInput input) {
        int ctx = llm.contextWindow();
        String user = "### Texte du scénario alternatif\n"    + trunc(input.text(),        ctx * 4 * 55 / 100)
            + "\n\n### Contraintes physiques/monde\n"         + trunc(input.constraints(), ctx * 4 / 8)
            + "\n\nÉvalue la plausibilité physique et causale. Conclus par SCORE: N.";
        String raw = llm.generate(SYSTEM, user, 0.3, LlmCallContext.of(agentName(), agentLabel())).text();
        List<String> problems = ProblemScoreParser.parseProblems(raw);
        double score = ProblemScoreParser.parseScore(raw);
        return new TextWhatIfCriticOutput(problems, score);
    }

    private static String trunc(String s, int maxChars) {
        if (s == null || s.isBlank()) return "";
        return s.length() <= maxChars ? s : s.substring(0, maxChars) + "…";
    }
}
