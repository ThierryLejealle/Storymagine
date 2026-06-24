package storymagine.redacteur.coeur.domaine.agent.writer.sequencestylechecker;

import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.redacteur.coeur.domaine.agent.Agent;
import storymagine.redacteur.coeur.domaine.agent.commun.ProblemScoreParser;

import java.util.List;

/**
 * Evaluates the stylistic quality of a single sequence.
 * Strict grader: reserves high scores for genuinely good prose.
 * Source: SequenceStyleCheckerContext.evaluate.
 */
public class SequenceStyleChecker implements Agent {

    private static final String AGENT_NAME = "SequenceStyleChecker";

    private final ModelCallPort llm;

    @Override
    public String agentName() { return AGENT_NAME; }

    public SequenceStyleChecker(ModelCallPort llm) {
        this.llm = llm;
    }

    public SequenceStyleCheckerOutput call(SequenceStyleCheckerInput input) {
        String system = buildSystem(input);
        String user   = buildUser(input);
        String raw    = llm.generate(system, user, 0.2, LlmCallContext.of(agentName())).text();
        List<String> problems = ProblemScoreParser.parseProblems(raw);
        int score = ProblemScoreParser.parseScoreInt(raw);
        return new SequenceStyleCheckerOutput(problems, score);
    }

    private String buildSystem(SequenceStyleCheckerInput in) {
        StringBuilder sys = new StringBuilder(
            "Tu es un éditeur littéraire exigeant et sans concession.\n");
        if (in.styleGuide() != null && !in.styleGuide().isBlank()) {
            sys.append("\n## Consigne de style\n")
               .append("Vérifie que le texte respecte scrupuleusement le guide de style ci-joint.\n")
               .append("Ne signale jamais comme défaut ce que le guide prescrit explicitement.\n");
        }
        sys.append("\n## Qualité stylistique\n")
           .append("Identifie sans pitié tout ce qui trahit une écriture artificielle ou de faible qualité :\n")
           .append("- Verbes faibles ou abstraits là où un verbe physique suffirait\n")
           .append("- Constructions nominalisées ou passives inutiles\n")
           .append("- Répétitions de structure ou de tournure dans le même passage\n")
           .append("- Formules génériques ou clichés de style (\"un sourire triste\", \"le cœur lourd\")\n")
           .append("- Adjectifs de remplissage sans pouvoir évocateur\n")
           .append("- Transitions mécaniques ou coutures visibles entre séquences\n")
           .append("- Phrases qui sonnent fabriquées plutôt que vécues\n");
        if (in.styleGuide() != null && !in.styleGuide().isBlank())
            sys.append("Si un de ces défauts est imposé par la consigne de style, ne le mentionne pas.\n");
        sys.append("\n## Échelle de notation\n")
           .append("10 = texte publiable tel quel — irréprochable\n")
           .append(" 8 = bon texte, défauts mineurs sans impact réel sur la lecture\n")
           .append(" 7 = correct mais plat ou sans relief — manque d'ambition stylistique\n")
           .append(" 6 = problèmes qui nuisent à la lecture ou cassent l'immersion\n")
           .append(" 5 = plusieurs défauts sérieux — réécriture partielle nécessaire\n")
           .append(" 3 = à réécrire intégralement sur le plan stylistique\n")
           .append(" 1 = texte qui trahit visiblement sa fabrication\n")
           .append("Un texte moyen ne mérite pas plus de 6. Réserve 8+ à l'exceptionnel.\n\n")
           .append("Format de sortie strict :\n")
           .append("PROBLEME: [description courte et précise]\n")
           .append("SCORE: N  (entier 0-10)\n")
           .append("En français. Sois précis et sévère — une note indulgente est inutile.");
        return sys.toString();
    }

    private String buildUser(SequenceStyleCheckerInput in) {
        int ctx      = llm.contextWindow();
        int textSlot = ctx * 4 * 50 / 100;
        int guideSlot = ctx * 4 / 8;
        int exSlot    = ctx * 4 / 6;
        StringBuilder usr = new StringBuilder();
        if (in.styleGuide() != null && !in.styleGuide().isBlank())
            usr.append("### Guide de style\n").append(trunc(in.styleGuide(), guideSlot)).append("\n\n");
        if (in.qualityCriteria() != null && !in.qualityCriteria().isBlank())
            usr.append("### Critères de qualité\n").append(trunc(in.qualityCriteria(), guideSlot)).append("\n\n");
        if (in.writingExample() != null && !in.writingExample().isBlank())
            usr.append("### Exemple de référence (style attendu)\n").append(trunc(in.writingExample(), exSlot)).append("\n\n");
        usr.append("### Texte à évaluer\n").append(trunc(in.text(), textSlot))
           .append("\n\nÉvalue ce texte. Conclus par SCORE: N.");
        return usr.toString();
    }

    private static String trunc(String s, int maxChars) {
        if (s == null || s.isBlank()) return "";
        return s.length() <= maxChars ? s : s.substring(0, maxChars) + "…";
    }
}
