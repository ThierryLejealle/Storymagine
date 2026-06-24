package storymagine.redacteur.coeur.domaine.agent.writer.textdreamcritic;

import storymagine.commun.coeur.ports.LlmCallContext;
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
public class TextDreamCritic implements Agent {

    private static final String AGENT_NAME = "TextDreamCritic";

    private final ModelCallPort llm;

    @Override
    public String agentName() { return AGENT_NAME; }

    public TextDreamCritic(ModelCallPort llm) {
        this.llm = llm;
    }

    public TextDreamCriticOutput call(TextDreamCriticInput input) {
        String level = (input.realismLevel() != null && !input.realismLevel().isBlank())
            ? input.realismLevel() : "symbolic";
        String levelCriteria = switch (level) {
            case "realistic" -> "Critères UNIQUEMENT : intensité émotionnelle (angoisses et désirs transparaissent dans des images familières légèrement décalées), cohérence distordue propre au rêve, résonance psychologique avec le personnage.";
            case "surreal"   -> "Critères UNIQUEMENT : originalité radicale des images (rien de banal ni de prévisible), logique interne propre au rêve (même absurde, il a sa cohérence), puissance sensorielle et émotionnelle.";
            default          -> "Critères UNIQUEMENT : puissance symbolique des images (archétypes, métaphores), résonance émotionnelle avec la psychologie du personnage, cohérence interne du rêve (sa propre logique).";
        };

        String system = "Tu évalues une SÉQUENCE DE RÊVE — niveau de réalisme : " + level + ".\n"
            + levelCriteria + "\n"
            + "Ignore totalement les lois physiques, la vraisemblance historique et la cohérence\n"
            + "avec la trame principale du roman — c'est un rêve, rien de cela n'est évalué.\n\n"
            + "Échelle de notation :\n"
            + "10 = rêve parfaitement évocateur\n 9 = excellent\n 8 = bon\n"
            + " 7 = atmosphère onirique présente mais images banales\n"
            + " 6 = rêve trop rationnel ou trop littéral\n"
            + " 5 = faible évocation\n 3 = à réécrire\n\n"
            + "Format de sortie strict :\n"
            + "PROBLEME: [défaut ou axe d'amélioration]\n"
            + "SCORE: N  (entier 0-10)\n"
            + "En français.";

        int ctx = llm.contextWindow();
        String user = "### Texte du rêve\n"                      + trunc(input.text(),    ctx * 4 * 55 / 100)
            + "\n\n### Psychologie du personnage (objectif du livre)\n" + trunc(input.bookGoal(), ctx * 4 / 8)
            + "\n\nÉvalue la qualité onirique. Conclus par SCORE: N.";

        String raw = llm.generate(system, user, 0.3, LlmCallContext.of(agentName())).text();
        List<String> problems = ProblemScoreParser.parseProblems(raw);
        double score = ProblemScoreParser.parseScore(raw);
        return new TextDreamCriticOutput(problems, score);
    }

    private static String trunc(String s, int maxChars) {
        if (s == null || s.isBlank()) return "";
        return s.length() <= maxChars ? s : s.substring(0, maxChars) + "…";
    }
}
