package storymagine.redacteur.coeur.domaine.agent.sequence.deusinmachinacorrector;

import storymagine.commun.coeur.domaine.prompt.PromptBuilder;
import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.redacteur.coeur.domaine.agent.AgentCorrector;
import storymagine.redacteur.coeur.domaine.agent.commun.CorrectionParser;
import storymagine.redacteur.coeur.domaine.agent.commun.RetryStrategy;

import java.util.List;

/**
 * Patches deus-ex-machina passages directly by returning (wrong → correct) pairs.
 * Detects the same 5 leak types as DeusInMachinaCritic, but instead of listing them,
 * returns corrected rewrites for inline substitution.
 * Source: DeusInMachinaContext.
 */
public class DeusInMachinaCorrector implements AgentCorrector {

    private static final String SYSTEM = """
            Tu lis un texte de roman et corriges les passages qui brisent l'immersion narrative —
            les endroits où la mécanique de fabrication est devenue visible dans la prose.

            RÈGLE PRÉALABLE — COHÉRENCE AVEC LE SCÉNARIO
            Si une "Consigne de séquence" ou un "Plan de séquence" sont fournis dans les données,
            lis-les en premier. Un passage qui réalise fidèlement ce qui y est demandé n'est PAS
            une fuite : c'est le moteur narratif qui fonctionne.
            Exception : si la formulation elle-même révèle une étiquette de personnage,
            une case cochée ou un artefact de fabrication, c'est une fuite même si le
            contenu était demandé.

            PRINCIPE
            Ce texte a été produit par un LLM à partir de consignes de rédaction.
            Une fuite est tout passage qui révèle au lecteur l'existence de ces consignes.
            Elle prend cinq formes, détaillées ci-dessous.
            TEST pour chaque phrase suspecte : cette phrase existerait-elle si aucune
            consigne ne l'avait provoquée ? Si non, c'est une fuite à corriger.

            CINQ FORMES DE FUITES

            1. NÉGATION VERBALISÉE
            Une consigne interdit X → le texte mentionne l'absence de X au lieu de simplement ne pas en parler.
              RÈGLE : ne corriger que si X correspond à une contrainte listée dans les consignes fournies.
              FUITE : "Il n'y eut pas de confrontation entre eux." (si consigne interdit les confrontations)
              OK    : "Les deux hommes se séparèrent sans un mot."

            2. FICHE PERSONNAGE DANS LA BOUCHE DU PERSONNAGE
            Un trait de personnage réapparaît dans le texte comme étiquette permanente plutôt qu'observation vivante.
              RÈGLE : concerne uniquement les traits de caractère ou de comportement étiquetés comme permanents.
              Les descriptions physiques ponctuelles ne sont PAS des fuites.
              FUITE : "Bertrand, taciturne comme toujours, garda le silence."
              FUITE : "Grâce à sa nature facile à vivre, il maintint son calme."
              OK    : "Bertrand fixa ses mains sans répondre."
              OK    : "Ses yeux noisette, légèrement myopes, lui donnaient un air rêveur."

            3. ARTEFACT DE SCÉNARIO
            Mots ou tournures qui appartiennent au script de fabrication, pas à la fiction :
            un fragment de consigne recopié dans la prose, ou une confirmation qu'une
            instruction a été suivie ("comme demandé", "conformément au plan", etc.).
              RÈGLE : observer le rythme ou la dynamique d'une scène n'est pas un artefact.
              FUITE : "Dans cette scène, Pierre comprend que..."
              FUITE : "Conformément au plan prévu, Marie révèle enfin son secret."
              OK    : "Pierre s'arrêta net. La vérité venait de le frapper."
              OK    : "...marquant une pause nette dans leurs échanges."

            4. LISTE NARRATIVISÉE
            Plusieurs phrases SÉPARÉES dont chacune coche une case — aucune n'a de poids propre.
              RÈGLE ABSOLUE : une liste introduite par deux-points (:) dans UNE seule phrase n'est JAMAIS type 4.
              Au minimum 4 phrases SÉPARÉES sont requises.
              FUITE : "Pierre arriva. Il observa la pièce. Il déposa sa valise. Il chercha son contact."

            5. ABSENCE JUSTIFIÉE
            Le texte explique pourquoi quelque chose n'arrive pas — justification qui ne s'explique que par une contrainte reçue.
              FUITE : "Il n'y eut pas de combat — la scène devait rester apaisée."

            FORMAT DE RÉPONSE

            Pour chaque fuite détectée :
            CORRECTIONS:
            - FAUX: "citation exacte du passage problématique"
              JUSTE: "réécriture corrigée du même passage"

            Pour une LISTE NARRATIVISÉE (type 4), cite le bloc complet de phrases :
            - FAUX: "Pierre arriva. Il observa la pièce. Il déposa sa valise. Il chercha son contact."
              JUSTE: "Pierre posa sa valise et parcourut la pièce des yeux."

            JUSTE ne change que ce qui constitue la fuite : conserve le sens, l'intention et les
            faits du passage et du scénario — ne réécris rien d'autre.

            FAUX doit être recopié mot pour mot, en entier — jamais de "..." pour abréger un passage long.
            Ni FAUX ni JUSTE ne portent de commentaire ou de justification après la citation.
              MAUVAIS : - FAUX: "...un point de repère humain." (Supprime l'étiquette d'action mécanique.)
              BON     : - FAUX: "...un point de repère humain."

            Si le texte est propre : PAS DE CORRECTION — rien d'autre.
            En français. Sois précis et minutieux pour trouver toutes les fuites avérées, même mineures.""";

    private static final String AGENT_NAME = "SequenceDeusInMachinaCorrector";

    private final ModelCallPort llm;

    @Override
    public String agentName() { return AGENT_NAME; }

    @Override
    public RetryStrategy retryStrategy() { return RetryStrategy.DECREASING_AND_RATIO_THRESHOLD; }

    public DeusInMachinaCorrector(ModelCallPort llm) {
        this.llm = llm;
    }

    public DeusInMachinaCorrectorOutput call(DeusInMachinaCorrectorInput input) {
        String user = PromptBuilder.create()
                .section("Consigne de séquence", input.sequenceDirective())
                .section("Plan de séquence",     input.sequencePlan())
                .raw(input.checks() == null || input.checks().isBlank()
                        ? "" : "Points de vérification actifs (pour référence) :\n" + input.checks())
                .raw("Texte à corriger :\n" + input.text())
                .raw("Corrige les fuites mécaniques. Réponds CORRECTIONS: (avec liste) ou PAS DE CORRECTION.")
                .build();
        String raw = llm.generate(SYSTEM, user, 0.2, LlmCallContext.of(agentName(), agentLabel()).withThink(thinks())).text().trim();
        return new DeusInMachinaCorrectorOutput(parseFindings(raw));
    }

    private static List<DeusInMachinaCorrectorFinding> parseFindings(String response) {
        return CorrectionParser.parse(response, "PAS DE CORRECTION", DeusInMachinaCorrectorFinding::new);
    }
}
