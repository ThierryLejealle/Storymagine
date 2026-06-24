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

    private static final String SYSTEM =
        "Tu lis un texte de roman et détectes les passages qui brisent l'immersion narrative —\n"
        + "les endroits où la mécanique de fabrication est devenue visible dans la prose.\n\n"
        + "RÈGLE PRÉALABLE — COHÉRENCE AVEC LE SCÉNARIO\n"
        + "Si une \"Consigne de séquence\" ou un \"Plan de séquence\" sont fournis dans les données,\n"
        + "lis-les en premier. Un passage qui réalise fidèlement ce qui y est demandé n'est PAS\n"
        + "une fuite : c'est le moteur narratif qui fonctionne. La fuite n'existe que si la\n"
        + "mécanique de fabrication devient visible au-delà ou indépendamment de la consigne reçue.\n\n"
        + "PRINCIPE\n"
        + "Un lecteur n'a pas accès aux instructions qui ont créé ce texte. Toute phrase qui ne\n"
        + "s'explique que si l'on connaît ces instructions est une fuite.\n\n"
        + "TEST : pour chaque phrase suspecte, demande-toi — cette phrase existerait-elle si\n"
        + "aucune consigne ne l'avait provoquée ? Si non, c'est une fuite.\n\n"
        + "────────────────────────────────────────────────────────────\n"
        + "CINQ FORMES DE FUITES\n"
        + "────────────────────────────────────────────────────────────\n\n"
        + "1. NÉGATION VERBALISÉE\n"
        + "Une consigne interdit X → le texte mentionne l'absence de X au lieu de simplement ne pas en parler.\n"
        + "  RÈGLE : ne signaler que si X correspond à une contrainte listée dans les consignes fournies.\n\n"
        + "2. FICHE PERSONNAGE DANS LA BOUCHE DU PERSONNAGE\n"
        + "Un trait de personnage réapparaît dans le texte comme étiquette permanente plutôt qu'observation vivante.\n\n"
        + "3. ARTEFACT DE SCÉNARIO\n"
        + "Mots ou tournures qui appartiennent au script de fabrication, pas à la fiction.\n"
        + "  FUITE : \"Dans cette scène, Pierre comprend que...\"\n"
        + "  OK    : Tout ce qu'un roman publié en librairie pourrait contenir.\n\n"
        + "4. LISTE NARRATIVISÉE\n"
        + "Plusieurs phrases SÉPARÉES dont chacune coche une case — aucune n'a de poids propre.\n"
        + "  RÈGLE ABSOLUE : une liste introduite par deux-points (:) dans UNE seule phrase n'est JAMAIS type 4.\n"
        + "  Au minimum 4 phrases SÉPARÉES sont requises.\n\n"
        + "5. ABSENCE JUSTIFIÉE\n"
        + "Le texte explique pourquoi quelque chose n'arrive pas — justification qui ne s'explique que par une contrainte reçue.\n\n"
        + "────────────────────────────────────────────────────────────\n"
        + "FORMAT DE RÉPONSE\n"
        + "────────────────────────────────────────────────────────────\n\n"
        + "Si tu détectes des fuites :\n"
        + "FUITE\n"
        + "- \"[citation exacte, courte]\" → type [1-5] — [une ligne d'explication]\n\n"
        + "Si le texte est propre :\n"
        + "OK\n\n"
        + "Sois précis et sélectif. En français.";

    private static final String AGENT_NAME = "DeusInMachinaChecker";

    private final ModelCallPort llm;

    @Override
    public String agentName() { return AGENT_NAME; }

    public DeusInMachinaChecker(ModelCallPort llm) {
        this.llm = llm;
    }

    public DeusInMachinaCheckerOutput call(DeusInMachinaCheckerInput input) {
        StringBuilder user = new StringBuilder();
        if (input.constraints() != null && !input.constraints().isBlank()) {
            user.append("Contraintes de rédaction actives (pour référence) :\n")
                .append(input.constraints()).append("\n\n");
        }
        user.append("Texte à analyser :\n").append(input.text())
            .append("\n\nRéponds FUITE (avec liste) ou OK.");
        String raw = llm.generate(SYSTEM, user.toString(), 0.2, LlmCallContext.of(agentName())).text().trim();
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
