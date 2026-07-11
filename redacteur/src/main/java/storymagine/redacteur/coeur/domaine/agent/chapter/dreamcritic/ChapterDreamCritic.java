package storymagine.redacteur.coeur.domaine.agent.chapter.dreamcritic;

import storymagine.commun.coeur.domaine.text.TruncHelper;
import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.LogPort;
import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.redacteur.coeur.domaine.agent.Agent;
import storymagine.redacteur.coeur.domaine.agent.commun.ProblemScoreParser;

import java.util.List;

/**
 * Evaluates the oneiric quality of a dream chapter.
 * Ignores physical laws and main-story coherence — calibrated to the declared realism level.
 * Levels: symbolic (default), realistic, surreal.
 * Source: CriticContext.evalDreamQuality.
 */
public class ChapterDreamCritic implements Agent {

    private static final String AGENT_NAME = "ChapterDreamCritic";

    private final ModelCallPort llm;
    private final LogPort       log;

    @Override
    public String agentName() { return AGENT_NAME; }

    public ChapterDreamCritic(ModelCallPort llm, LogPort log) {
        this.llm = llm;
        this.log = log;
    }

    public ChapterDreamCriticOutput call(ChapterDreamCriticInput input) {
        String level = (input.realismLevel() != null && !input.realismLevel().isBlank())
                ? input.realismLevel() : "symbolic";

        int ctx = llm.contextWindow();
        TruncHelper t = TruncHelper.create();
        String user = "### Texte du rêve\n"                                 + t.text(input.text(),     ctx * 4 * 55 / 100, "text")
                + "\n\n### Psychologie du personnage (objectif du livre)\n" + t.text(input.bookGoal(), ctx * 4 / 8, "bookGoal")
                + "\n\nÉvalue la qualité onirique. Conclus par SCORE: N.";
        t.logIfTruncated(log, agentName());

        String raw = llm.generate(buildSystem(level), user, 0.3, LlmCallContext.of(agentName(), agentLabel()).withThink(thinks())).text();
        List<String> problems = ProblemScoreParser.parseProblems(raw);
        double score = ProblemScoreParser.parseScore(raw);
        return new ChapterDreamCriticOutput(problems, score);
    }

    private static String buildSystem(String level) {
        String levelCriteria = switch (level) {
            case "realistic" -> "Critères UNIQUEMENT : intensité émotionnelle (angoisses et désirs transparaissent dans des images familières légèrement décalées), cohérence distordue propre au rêve, résonance psychologique avec le personnage.";
            case "surreal"   -> "Critères UNIQUEMENT : originalité radicale des images (rien de banal ni de prévisible), logique interne propre au rêve (même absurde, il a sa cohérence), puissance sensorielle et émotionnelle.";
            default          -> "Critères UNIQUEMENT : puissance symbolique des images (archétypes, métaphores), résonance émotionnelle avec la psychologie du personnage, cohérence interne du rêve (sa propre logique).";
        };

        return "Tu évalues une SÉQUENCE DE RÊVE — niveau de réalisme : " + level + ".\n"
                + levelCriteria
                + """

                Ignore totalement les lois physiques, la vraisemblance historique et la cohérence
                avec la trame principale du roman — c'est un rêve, rien de cela n'est évalué.

                Échelle de notation :
                10 = rêve parfaitement évocateur
                 9 = excellent
                 8 = bon
                 7 = atmosphère onirique présente mais images banales
                 6 = rêve trop rationnel ou trop littéral
                 5 = faible évocation
                 3 = à réécrire

                Format de sortie strict :
                PROBLEME: [défaut ou axe d'amélioration]
                SCORE: N  (entier 0-10)
                En français.""";
    }
}
