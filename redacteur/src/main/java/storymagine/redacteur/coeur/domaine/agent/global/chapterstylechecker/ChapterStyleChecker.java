package storymagine.redacteur.coeur.domaine.agent.global.chapterstylechecker;

import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.redacteur.coeur.domaine.agent.Agent;
import storymagine.redacteur.coeur.domaine.agent.commun.ProblemScoreParser;

import java.util.List;

/**
 * Evaluates the stylistic quality of an entire finished chapter.
 * Chapter-level equivalent of SequenceStyleChecker; stricter grading scale.
 * Source: ChapterStyleCheckerContext.evaluate.
 */
public class ChapterStyleChecker implements Agent {

    private static final String AGENT_NAME = "ChapterStyleChecker";

    private final ModelCallPort llm;

    @Override
    public String agentName() { return AGENT_NAME; }

    public ChapterStyleChecker(ModelCallPort llm) {
        this.llm = llm;
    }

    public ChapterStyleCheckerOutput call(ChapterStyleCheckerInput input) {
        String system = buildSystem(input);
        String user   = buildUser(input);
        String raw    = llm.generate(system, user, 0.2, LlmCallContext.of(agentName())).text();
        List<String> problems = ProblemScoreParser.parseProblems(raw);
        int score = ProblemScoreParser.parseScoreInt(raw);
        return new ChapterStyleCheckerOutput(problems, score);
    }

    private String buildSystem(ChapterStyleCheckerInput in) {
        boolean hasStyle = in.styleGuide() != null && !in.styleGuide().isBlank();

        // Section optionnelle : consigne de style (avant les critères qualité pour que le modèle la lise en premier)
        String styleGuideSection = !hasStyle ? "" : "\n\n## Consigne de style\n"
                + "Vérifie que le texte respecte scrupuleusement le guide de style ci-joint.\n"
                + "Ne signale jamais comme défaut ce que le guide prescrit explicitement.";

        String qualitySection = """

                ## Qualité stylistique
                Identifie tout ce qui trahit une écriture artificielle ou de faible qualité :
                - Verbes faibles ou abstraits là où un verbe physique suffirait
                - Constructions nominalisées ou passives inutiles
                - Répétitions de structure ou de tournure dans le même passage
                - Formules génériques ou clichés de style
                - Adjectifs de remplissage sans pouvoir évocateur
                - Transitions mécaniques ou coutures visibles
                - Phrases qui sonnent fabriquées plutôt que vécues""";

        // Note optionnelle : exception si un défaut est imposé par la consigne de style
        String styleException = !hasStyle ? ""
                : "\nSi un de ces défauts est imposé par la consigne de style, ne le mentionne pas.";

        String notationSection = """

                ## Échelle de notation
                10 = parfait, rien à retoucher sur le plan stylistique
                 9 = excellent
                 8 = bon
                 7 = lisible mais plusieurs maladresses
                 6 = correct mais largement améliorable
                 5 = moyen
                 3 = à réécrire intégralement
                Sois strict : réserve 8+ à un texte vraiment bon.

                Format de sortie strict :
                PROBLEME: [défaut ou axe d'amélioration stylistique]
                SCORE: N  (entier 0-10)
                En français. Sois précis et sévère.""";

        return "Tu es un éditeur littéraire exigeant et sans concession."
                + styleGuideSection
                + qualitySection
                + styleException
                + notationSection;
    }

    private String buildUser(ChapterStyleCheckerInput in) {
        int ctx       = llm.contextWindow();
        int textSlot  = ctx * 4 * 55 / 100;
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
