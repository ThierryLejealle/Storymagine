package storymagine.redacteur.coeur.domaine.agent.temp.chapterstylecritic;

import storymagine.commun.coeur.domaine.text.TruncHelper;
import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.LogPort;
import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.redacteur.coeur.domaine.agent.Agent;
import storymagine.redacteur.coeur.domaine.agent.commun.ProblemScoreParser;

import java.util.List;

/**
 * Evaluates the stylistic quality of an entire finished chapter.
 * Chapter-level equivalent of SequenceStyleCorrector; stricter grading scale.
 * Source: ChapterStyleCheckerContext.evaluate.
 */
public class ChapterStyleCritic implements Agent {

    private static final String AGENT_NAME = "ChapterStyleCritic";

    private final ModelCallPort llm;
    private final LogPort       log;

    @Override
    public String agentName() { return AGENT_NAME; }

    public ChapterStyleCritic(ModelCallPort llm, LogPort log) {
        this.llm = llm;
        this.log = log;
    }

    public ChapterStyleCriticOutput call(ChapterStyleCriticInput input) {
        String system = buildSystem(input);
        String user   = buildUser(input);
        String raw    = llm.generate(system, user, 0.2, LlmCallContext.of(agentName(), agentLabel()).withThink(thinks())).text();
        List<String> problems = ProblemScoreParser.parseProblems(raw);
        int score = ProblemScoreParser.parseScoreInt(raw);
        return new ChapterStyleCriticOutput(problems, score);
    }

    private String buildSystem(ChapterStyleCriticInput in) {
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
                Identifie tout ce qui trahit une écriture artificielle ou de faible qualité :
                - Verbes faibles ou abstraits là où un verbe physique suffirait
                - Constructions nominalisées ou passives inutiles
                - Répétitions de structure ou de tournure dans le même passage
                - Formules génériques ou clichés de style
                - Adjectifs de remplissage sans pouvoir évocateur
                - Transitions mécaniques ou coutures visibles
                - Phrases qui sonnent fabriquées plutôt que vécues""";

        String notationSection = """

                ## Échelle de notation
                10 = parfait, rien à retoucher sur le plan stylistique
                 9 = excellent
                 8 = bon
                 7 = lisible mais plusieurs maladresses
                 6 = correct mais largement améliorable
                 5 = moyen
                 4 = mauvais
                 3 = très mauvais
                 2 = nul
                 1 = absolument nul
                Sois strict : réserve 8+ à un texte vraiment bon.

                Format de sortie strict :
                PROBLEME: [défaut ou axe d'amélioration stylistique]
                SCORE: N  (entier 1-10)
                En français. Sois précis et sévère.""";

        return "Tu es un éditeur littéraire exigeant et sans concession."
                + styleGuideSection
                + qualitySection
                + notationSection;
    }

    private String buildUser(ChapterStyleCriticInput in) {
        int ctx       = llm.contextWindow();
        int textSlot  = ctx * 4 * 55 / 100;
        int guideSlot = ctx * 4 / 8;
        int exSlot    = ctx * 4 / 6;

        TruncHelper t = TruncHelper.create();
        String styleSection    = (in.styleGuide()      != null && !in.styleGuide().isBlank())      ? "### Guide de style\n"                       + t.text(in.styleGuide(),      guideSlot, "styleGuide")      + "\n\n" : "";
        String criteriaSection = (in.qualityCriteria() != null && !in.qualityCriteria().isBlank()) ? "### Critères de qualité\n"                  + t.text(in.qualityCriteria(), guideSlot, "qualityCriteria") + "\n\n" : "";
        String exampleSection  = (in.writingExample()  != null && !in.writingExample().isBlank())  ? "### Exemple de référence (style attendu)\n" + t.text(in.writingExample(),  exSlot, "writingExample")    + "\n\n" : "";

        String user = styleSection
                + criteriaSection
                + exampleSection
                + "### Texte à évaluer\n" + t.text(in.text(), textSlot, "text")
                + "\n\nÉvalue ce texte. Conclus par SCORE: N.";
        t.logIfTruncated(log, agentName());
        return user;
    }
}
