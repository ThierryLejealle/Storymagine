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
 *
 * TODO: décider si le SCORE LLM est remplacé par scoreFromProblemCount (comme GoalPlanChecker)
 *       ou conservé — ici l'échelle est qualitative ("plat mais sans défaut" = 7 avec 0 PROBLEME:).
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
        boolean hasStyle = in.styleGuide() != null && !in.styleGuide().isBlank();

        // Section optionnelle : consigne de style (avant les critères qualité pour que le modèle la lise en premier)
        String styleGuideSection = !hasStyle ? "" : "\n\n## Consigne de style\n"
                + "Vérifie que le texte respecte scrupuleusement le guide de style ci-joint.\n"
                + "Ne signale jamais comme défaut ce que le guide prescrit explicitement";
                
        // Note optionnelle : exception si un défaut est imposé par la consigne de style
        if (hasStyle) styleGuideSection += ", par exemple, un défaut imposé par la consigne de style";
        styleGuideSection += ".";

        String qualitySection = """

                ## Qualité stylistique
                Identifie sans pitié tout ce qui trahit une écriture artificielle ou de faible qualité :
                - Verbes faibles ou abstraits là où un verbe physique suffirait
                - Constructions nominalisées ou passives inutiles
                - Répétitions de structure ou de tournure dans le même passage
                - Formules génériques ou clichés de style ("un sourire triste", "le cœur lourd")
                - Adjectifs de remplissage sans pouvoir évocateur
                - Transitions mécaniques ou coutures visibles entre séquences
                - Phrases qui sonnent fabriquées plutôt que vécues""";

        String notationSection = """

                ## Échelle de notation
                10 = texte publiable tel quel — irréprochable
                 9 = excellent, au mieux un remarque mineure
                 8 = bon texte, défauts mineurs sans impact réel sur la lecture
                 7 = correct mais plat ou sans relief — manque d'ambition stylistique
                 6 = problèmes qui nuisent à la lecture ou cassent l'immersion
                 5 = plusieurs défauts sérieux — réécriture partielle nécessaire
                 4 = défauts graves
                 3 = à réécrire intégralement sur le plan stylistique
                 2 = nul
                 1 = absolument nul. texte qui trahit totalement sa fabrication
                Un texte moyen ne mérite pas plus de 6. Réserve 8+ à l'exceptionnel.

                Format de sortie strict :
                PROBLEME: [description courte et précise]
                SCORE: N  (entier 1-10)
                En français. Sois précis et sévère — une note indulgente est inutile.""";

        return "Tu es un éditeur littéraire exigeant et sans concession."
                + styleGuideSection
                + qualitySection
                + notationSection;
    }

    private String buildUser(SequenceStyleCheckerInput in) {
        int ctx       = llm.contextWindow();
        int textSlot  = ctx * 4 * 50 / 100;
        int guideSlot = ctx * 4 / 8;
        int exSlot    = ctx * 4 / 6;

        String styleSection    = (in.styleGuide()      != null && !in.styleGuide().isBlank())      ? "### Guide de style\n"                       + trunc(in.styleGuide(),      guideSlot) + "\n\n" : "";
        String criteriaSection = (in.qualityCriteria() != null && !in.qualityCriteria().isBlank()) ? "### Critères de qualité\n"                  + trunc(in.qualityCriteria(), guideSlot) + "\n\n" : "";
        String exampleSection  = (in.writingExample()  != null && !in.writingExample().isBlank())  ? "### Exemple de référence (style attendu)\n" + trunc(in.writingExample(),  exSlot)    + "\n\n" : "";

        return styleSection
                + criteriaSection
                + exampleSection
                + "### Texte à évaluer\n" + trunc(in.text(), textSlot)
                + "\n\nÉvalue ce texte. Conclus par SCORE: N.";
    }

    private static String trunc(String s, int maxChars) {
        if (s == null || s.isBlank()) return "";
        return s.length() <= maxChars ? s : s.substring(0, maxChars) + "…";
    }
}
