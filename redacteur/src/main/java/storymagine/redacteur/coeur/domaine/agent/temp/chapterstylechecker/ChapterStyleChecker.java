package storymagine.redacteur.coeur.domaine.agent.temp.chapterstylechecker;

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
        String raw    = llm.generate(system, user, 0.2, LlmCallContext.of(agentName(), agentLabel())).text();
        List<String> problems = ProblemScoreParser.parseProblems(raw);
        int score = ProblemScoreParser.parseScoreInt(raw);
        return new ChapterStyleCheckerOutput(problems, score);
    }

    private String buildSystem(ChapterStyleCheckerInput in) {
        boolean hasStyle = in.styleGuide() != null && !in.styleGuide().isBlank();

        // Section optionnelle : consigne de style (avant les critÃ¨res qualitÃ© pour que le modÃ¨le la lise en premier)
        String styleGuideSection = !hasStyle ? "" : "\n\n## Consigne de style\n"
                + "VÃ©rifie que le texte respecte scrupuleusement le guide de style ci-joint.\n"
                + "Ne signale jamais comme dÃ©faut ce que le guide prescrit explicitement";
        // Note optionnelle : exception si un dÃ©faut est imposÃ© par la consigne de style
        if (hasStyle) styleGuideSection += ", par exemple, un dÃ©faut imposÃ© par la consigne de style";
        styleGuideSection += ".";

        String qualitySection = """

                ## QualitÃ© stylistique
                Identifie tout ce qui trahit une Ã©criture artificielle ou de faible qualitÃ© :
                - Verbes faibles ou abstraits lÃ  oÃ¹ un verbe physique suffirait
                - Constructions nominalisÃ©es ou passives inutiles
                - RÃ©pÃ©titions de structure ou de tournure dans le mÃªme passage
                - Formules gÃ©nÃ©riques ou clichÃ©s de style
                - Adjectifs de remplissage sans pouvoir Ã©vocateur
                - Transitions mÃ©caniques ou coutures visibles
                - Phrases qui sonnent fabriquÃ©es plutÃ´t que vÃ©cues""";

        String notationSection = """

                ## Ã‰chelle de notation
                10 = parfait, rien Ã  retoucher sur le plan stylistique
                 9 = excellent
                 8 = bon
                 7 = lisible mais plusieurs maladresses
                 6 = correct mais largement amÃ©liorable
                 5 = moyen
                 4 = mauvais
                 3 = trÃ¨s mauvais
                 2 = nul
                 1 = absolument nul
                Sois strict : rÃ©serve 8+ Ã  un texte vraiment bon.

                Format de sortie strict :
                PROBLEME: [dÃ©faut ou axe d'amÃ©lioration stylistique]
                SCORE: N  (entier 1-10)
                En franÃ§ais. Sois prÃ©cis et sÃ©vÃ¨re.""";

        return "Tu es un Ã©diteur littÃ©raire exigeant et sans concession."
                + styleGuideSection
                + qualitySection
                + notationSection;
    }

    private String buildUser(ChapterStyleCheckerInput in) {
        int ctx       = llm.contextWindow();
        int textSlot  = ctx * 4 * 55 / 100;
        int guideSlot = ctx * 4 / 8;
        int exSlot    = ctx * 4 / 6;

        String styleSection    = (in.styleGuide()      != null && !in.styleGuide().isBlank())      ? "### Guide de style\n"                       + trunc(in.styleGuide(),      guideSlot) + "\n\n" : "";
        String criteriaSection = (in.qualityCriteria() != null && !in.qualityCriteria().isBlank()) ? "### CritÃ¨res de qualitÃ©\n"                  + trunc(in.qualityCriteria(), guideSlot) + "\n\n" : "";
        String exampleSection  = (in.writingExample()  != null && !in.writingExample().isBlank())  ? "### Exemple de rÃ©fÃ©rence (style attendu)\n" + trunc(in.writingExample(),  exSlot)    + "\n\n" : "";

        return styleSection
                + criteriaSection
                + exampleSection
                + "### Texte Ã  Ã©valuer\n" + trunc(in.text(), textSlot)
                + "\n\nÃ‰value ce texte. Conclus par SCORE: N.";
    }

    private static String trunc(String s, int maxChars) {
        if (s == null || s.isBlank()) return "";
        return s.length() <= maxChars ? s : s.substring(0, maxChars) + "â€¦";
    }
}
