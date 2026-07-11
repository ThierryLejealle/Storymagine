package storymagine.redacteur.coeur.domaine.agent.sequence.stylecorrector;

import storymagine.commun.coeur.domaine.prompt.PromptBuilder;
import storymagine.commun.coeur.domaine.text.TruncHelper;
import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.LogPort;
import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.redacteur.coeur.domaine.agent.AgentCorrector;
import storymagine.redacteur.coeur.domaine.agent.commun.CorrectionParser;
import storymagine.redacteur.coeur.domaine.agent.commun.RetryStrategy;

import java.util.List;

/**
 * Corrects stylistic weaknesses directly via (FAUX → JUSTE) phrase pairs.
 * Does not explain problems — patches inline like other Correctors.
 */
public class StyleCorrector implements AgentCorrector {

    private static final String AGENT_NAME = "SequenceStyleCorrector";

    private final ModelCallPort llm;
    private final LogPort       log;

    @Override
    public String agentName() { return AGENT_NAME; }

    @Override
    public RetryStrategy retryStrategy() { return RetryStrategy.RATIO_THRESHOLD; }

    public StyleCorrector(ModelCallPort llm, LogPort log) {
        this.llm = llm;
        this.log = log;
    }

    public StyleCorrectorOutput call(StyleCorrectorInput input) {
        String system = buildSystem(input);
        String user   = buildUser(input);
        String raw    = llm.generate(system, user, 0.2, LlmCallContext.of(agentName(), agentLabel()).withThink(thinks())).text();
        return new StyleCorrectorOutput(parseFindings(raw));
    }

    private String buildSystem(StyleCorrectorInput in) {
        boolean hasStyle = in.styleGuide() != null && !in.styleGuide().isBlank();

        String styleGuideSection = !hasStyle ? "" : "\n\n## Consigne de style\n"
                + "Verifie que le texte respecte scrupuleusement le guide de style ci-joint.\n"
                + "Ne signale jamais comme defaut ce que le guide prescrit explicitement";
        if (hasStyle) styleGuideSection += ", par exemple, un defaut impose par la consigne de style";
        styleGuideSection += ".";

        String qualitySection = """

                ## Qualite stylistique
                Identifie tout ce qui peut etre ameliore ou corrige dans la prose — des preferences stylistiques mineures aux defauts graves.
                Ne signale pas une formulation uniquement parce qu'elle appartient a un registre different de l'exemple de reference : evalue le registre du texte en coherence avec son contexte.
                Cherche en priorite :
                - Verbes faibles ou abstraits la ou un verbe physique suffirait
                - Constructions nominalisees ou passives inutiles
                - Repetitions de structure, de tournure, ou d'un meme mot ou groupe significatif dans des phrases proches
                  (ex : "Il effaca la rature avec precipitation. Il prit sa gomme et effaca la rature." — "effaca la rature" repete en deux phrases)
                - Formules generiques ou cliches de style ("un sourire triste", "le coeur lourd")
                - Adjectifs de remplissage sans pouvoir evocateur
                - Transitions mecaniques ou coutures visibles entre sequences
                - Phrases qui sonnent fabriquees plutot que vecues""";

        String procedureSection = """

                PROCEDURE OBLIGATOIRE :
                1. Lis attentivement le texte et trouve tous les defauts et faiblesses stylistiques, meme mineurs.
                2. Qualifie chaque point :
                   AMELIORATION: la plupart des lecteurs ne remarqueront rien. Un bon editeur pourrait neanmoins proposer une retouche pour gagner en precision, en fluidite ou en originalite.
                   Exemple : "Un silence s'installa entre eux." — formule usee ; "Ils se turent." serait plus direct.
                   DEFAUT_SIGNIFICATIF: un lecteur attentif percoit une faiblesse de style ou une maladresse qui diminue la qualite de la prose, sans casser completement l'immersion.
                   Exemple : "semble" utilise quatre fois en deux paragraphes — affaiblit la presence des personnages.
                   DEFAUT_MAJEUR: le passage attire l'attention sur l'ecriture elle-meme, rompt l'immersion ou donne une impression manifeste de texte artificiel.
                   Exemple : "Leurs regards se croiserent dans un moment de comprehension mutuelle tacite." — surconstruit, analytique.
                   Pour chaque point, ecris uniquement la correction (pas d'explication).

                FORMAT STRICT :
                CORRECTIONS:
                - FAUX: "phrase exacte contenant le defaut"
                  JUSTE: "phrase corrigee"
                Si aucun defaut : PAS DE CORRECTION — rien d'autre.
                Rien d'autre : ni explication ni commentaire.

                Exemple 1 - defauts presents :
                CORRECTIONS:
                - FAUX: "Un silence s'installa entre eux."
                  JUSTE: "Ils se turent."
                - FAUX: "Leurs regards se croiserent dans un moment de comprehension mutuelle tacite."
                  JUSTE: "Leurs regards se croiserent."

                FAUX doit etre recopie mot pour mot, en entier — jamais de "..." pour abreger un passage long.
                Ni FAUX ni JUSTE ne portent de commentaire ou de justification apres la citation.
                  MAUVAIS : - FAUX: "...ce cadre idyllique." (Repetition structurelle)
                  BON     : - FAUX: "...ce cadre idyllique."

                Exemple 2 - aucun defaut :
                PAS DE CORRECTION""";

        return "Tu es un editeur litteraire exigeant et sans concession."
                + styleGuideSection
                + qualitySection
                + procedureSection;
    }

    private String buildUser(StyleCorrectorInput in) {
        int ctx       = llm.contextWindow();
        int textSlot  = ctx * 4 * 50 / 100;
        int guideSlot = ctx * 4 / 8;
        int exSlot    = ctx * 4 / 6;

        TruncHelper t = TruncHelper.create();
        String user = PromptBuilder.create()
                .section("Guide de style", t.text(in.styleGuide(), guideSlot, "styleGuide"))
                .section("Criteres de qualite", t.text(in.qualityCriteria(), guideSlot, "qualityCriteria"))
                .section("Exemple de reference (style attendu)", t.text(in.writingExample(), exSlot, "writingExample"))
                .section("Texte a corriger", t.text(in.text(), textSlot, "text"))
                .raw("Corrige ce texte. Donne les corrections FAUX/JUSTE.")
                .build();
        t.logIfTruncated(log, agentName());
        return user;
    }

    private static List<StyleCorrectorFinding> parseFindings(String response) {
        return CorrectionParser.parse(response, "PAS DE CORRECTION", StyleCorrectorFinding::new);
    }
}
