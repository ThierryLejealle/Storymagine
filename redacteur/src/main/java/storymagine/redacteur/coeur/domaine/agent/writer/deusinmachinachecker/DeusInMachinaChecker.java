package storymagine.redacteur.coeur.domaine.agent.writer.deusinmachinachecker;

import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.redacteur.coeur.domaine.agent.Agent;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects passages where the fabrication mechanics are visible in the prose
 * (instruction leaks, character-sheet labels, scenario artefacts, etc.).
 * Defines 5 leak types: NEGATION_VERBALISEE, FICHE_DANS_LA_BOUCHE,
 * ARTEFACT_DE_SCENARIO, LISTE_NARRATIVISEE, ABSENCE_JUSTIFIEE.
 * Source: DeusInMachinaContext.
 */
public class DeusInMachinaChecker implements Agent {

    private static final String SYSTEM = """
            Tu lis un texte de roman et détectes les passages qui brisent l'immersion narrative —
            les endroits où la mécanique de fabrication est devenue visible dans la prose.

            RÈGLE PRÉALABLE — COHÉRENCE AVEC LE SCÉNARIO
            Si une "Consigne de séquence" ou un "Plan de séquence" sont fournis dans les données,
            lis-les en premier. Un passage qui réalise fidèlement ce qui y est demandé n'est PAS
            une fuite : c'est le moteur narratif qui fonctionne. La fuite n'existe que si la
            mécanique de fabrication devient visible au-delà ou indépendamment de la consigne reçue.

            PRINCIPE
            Ce texte a été produit par un LLM à partir de consignes de rédaction.
            Les deux fuites principales à détecter :
            — un fragment de consigne recopié dans la prose
            — une confirmation que le LLM a suivi une instruction ("comme demandé", "conformément au plan", etc.)
            Tout passage qui révèle l'existence de ces consignes au lecteur est une fuite.

            TEST : pour chaque phrase suspecte, demande-toi — cette phrase existerait-elle si
            aucune consigne ne l'avait provoquée ? Si non, c'est une fuite.

            CINQ FORMES DE FUITES

            1. NÉGATION VERBALISÉE
            Une consigne interdit X → le texte mentionne l'absence de X au lieu de simplement ne pas en parler.
              RÈGLE : ne signaler que si X correspond à une contrainte listée dans les consignes fournies.
              FUITE : "Il n'y eut pas de confrontation entre eux." (si consigne interdit les confrontations)
              OK    : "Les deux hommes se séparèrent sans un mot."

            2. FICHE PERSONNAGE DANS LA BOUCHE DU PERSONNAGE
            Un trait de personnage réapparaît dans le texte comme étiquette permanente plutôt qu'observation vivante.
              FUITE : "Bertrand, taciturne comme toujours, garda le silence."
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

            Si tu détectes des fuites :
            FUITE
            - "[citation exacte, courte]" → type [1-5] — [une ligne d'explication]

            Si le texte est propre :
            OK

            Sois précis et sélectif. En français.""";

    private static final String AGENT_NAME = "DeusInMachinaChecker";

    private final ModelCallPort llm;

    @Override
    public String agentName() { return AGENT_NAME; }

    public DeusInMachinaChecker(ModelCallPort llm) {
        this.llm = llm;
    }

    public DeusInMachinaCheckerOutput call(DeusInMachinaCheckerInput input) {
        // Section optionnelle : contraintes actives (en premier, pour que le modèle les lise avant le texte)
        String constraintsSection = (input.constraints() != null && !input.constraints().isBlank())
                ? "Contraintes de rédaction actives (pour référence) :\n" + input.constraints() + "\n\n"
                : "";
        String user = constraintsSection
                + "Texte à analyser :\n" + input.text()
                + "\n\nRéponds FUITE (avec liste) ou OK.";
        String raw = llm.generate(SYSTEM, user, 0.2, LlmCallContext.of(agentName(), agentLabel())).text().trim();
        return new DeusInMachinaCheckerOutput(parseLeaks(raw));
    }

    private static List<String> parseLeaks(String response) {
        List<String> leaks = new ArrayList<>();
        if (response.startsWith("OK")) return leaks;
        for (String line : response.split("\n")) {
            String t = line.trim();
            if (t.startsWith("- ")) leaks.add(t.substring(2));
        }
        return leaks;
    }
}
