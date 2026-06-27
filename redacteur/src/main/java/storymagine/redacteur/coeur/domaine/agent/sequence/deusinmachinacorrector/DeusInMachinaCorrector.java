package storymagine.redacteur.coeur.domaine.agent.sequence.deusinmachinacorrector;

import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.redacteur.coeur.domaine.agent.Agent;

import java.util.ArrayList;
import java.util.List;

/**
 * Patches deus-ex-machina passages directly by returning (wrong → correct) pairs.
 * Detects the same 5 leak types as DeusInMachinaCritic, but instead of listing them,
 * returns corrected rewrites for inline substitution.
 * Source: DeusInMachinaContext.
 */
public class DeusInMachinaCorrector implements Agent {

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
            Les deux fuites principales à corriger :
            — un fragment de consigne recopié dans la prose
            — une confirmation que le LLM a suivi une instruction ("comme demandé", "conformément au plan", etc.)
            Tout passage qui révèle l'existence de ces consignes au lecteur est une fuite.

            TEST : pour chaque phrase suspecte, demande-toi — cette phrase existerait-elle si
            aucune consigne ne l'avait provoquée ? Si non, c'est une fuite à corriger.

            CINQ FORMES DE FUITES

            1. NÉGATION VERBALISÉE
            Une consigne interdit X → le texte mentionne l'absence de X au lieu de simplement ne pas en parler.
              RÈGLE : ne corriger que si X correspond à une contrainte listée dans les consignes fournies.
              FUITE : "Il n'y eut pas de confrontation entre eux." (si consigne interdit les confrontations)
              OK    : "Les deux hommes se séparèrent sans un mot."

            2. FICHE PERSONNAGE DANS LA BOUCHE DU PERSONNAGE
            Un trait de personnage réapparaît dans le texte comme étiquette permanente plutôt qu'observation vivante.
              FUITE : "Bertrand, taciturne comme toujours, garda le silence."
              FUITE : "Grâce à sa nature facile à vivre, il maintint son calme."
              OK    : "Bertrand fixa ses mains sans répondre."

            3. ARTEFACT DE SCÉNARIO
            Mots ou tournures qui appartiennent au script de fabrication, pas à la fiction.
              FUITE : "Dans cette scène, Pierre comprend que..."
              FUITE : "Conformément au plan prévu, Marie révèle enfin son secret."
              OK    : "Pierre s'arrêta net. La vérité venait de le frapper."

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

            Si le texte est propre : PAS DE CORRECTION — rien d'autre.
            En français. Sois précis et minutieux pour trouver toutes les fuites avérées, même mineures.""";

    private static final String AGENT_NAME = "SequenceDeusInMachinaCorrector";

    private final ModelCallPort llm;

    @Override
    public String agentName() { return AGENT_NAME; }

    public DeusInMachinaCorrector(ModelCallPort llm) {
        this.llm = llm;
    }

    public DeusInMachinaCorrectorOutput call(DeusInMachinaCorrectorInput input) {
        // Section optionnelle : contraintes actives (en premier, pour que le modèle les lise avant le texte)
        String constraintsSection = (input.constraints() != null && !input.constraints().isBlank())
                ? "Contraintes de rédaction actives (pour référence) :\n" + input.constraints() + "\n\n"
                : "";
        String user = constraintsSection
                + "Texte à corriger :\n" + input.text()
                + "\n\nCorrige les fuites mécaniques. Réponds CORRECTIONS: (avec liste) ou PAS DE CORRECTION.";
        String raw = llm.generate(SYSTEM, user, 0.2, LlmCallContext.of(agentName(), agentLabel())).text().trim();
        return new DeusInMachinaCorrectorOutput(parseFindings(raw));
    }

    private static List<DeusInMachinaCorrectorFinding> parseFindings(String response) {
        if (response == null || response.trim().startsWith("PAS DE CORRECTION")) return List.of();
        List<DeusInMachinaCorrectorFinding> result = new ArrayList<>();
        String[] lines = response.split("\n");
        String wrong = null;
        for (String line : lines) {
            String t = line.trim();
            if (t.startsWith("- FAUX:") || t.startsWith("FAUX:")) {
                wrong = unquote(t.replaceFirst("^-?\\s*FAUX:", "").trim());
            } else if (t.startsWith("JUSTE:") && wrong != null) {
                String correct = unquote(t.substring("JUSTE:".length()).trim());
                if (!wrong.isBlank() && !correct.isBlank())
                    result.add(new DeusInMachinaCorrectorFinding(wrong, correct));
                wrong = null;
            }
        }
        return result;
    }

    private static String unquote(String s) {
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\""))
            return s.substring(1, s.length() - 1);
        return s;
    }
}
