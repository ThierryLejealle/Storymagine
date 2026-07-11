package storymagine.redacteur.coeur.domaine.agent.sequence.deusinmachinacritic;

import storymagine.commun.coeur.domaine.prompt.PromptBuilder;
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
public class DeusInMachinaCritic implements Agent {

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
            Une fuite est tout passage qui révèle au lecteur l'existence de ces consignes.
            Elle prend cinq formes, détaillées ci-dessous.

            TEST : pour chaque phrase suspecte, demande-toi — cette phrase trahit-elle
            l'existence d'une consigne de rédaction ? Un romancier humain qui écrirait
            librement pourrait-il écrire exactement cela ? Si oui, ce n'est pas une fuite.

            CINQ FORMES DE FUITES

            1. NÉGATION VERBALISÉE
            Une consigne interdit X → le texte mentionne l'absence de X au lieu de simplement ne pas en parler.
              RÈGLE : ne signaler que si X correspond à une contrainte listée dans les consignes fournies.
              FUITE : "Il n'y eut pas de confrontation entre eux." (si consigne interdit les confrontations)
              OK    : "Les deux hommes se séparèrent sans un mot."

            2. FICHE PERSONNAGE DANS LA BOUCHE DU PERSONNAGE
            Un trait de personnage réapparaît dans le texte comme étiquette permanente plutôt qu'observation vivante.
              RÈGLE : concerne uniquement les traits de caractère ou de comportement étiquetés comme permanents.
              Les descriptions physiques ponctuelles ne sont PAS des fuites.
              FUITE : "Bertrand, taciturne comme toujours, garda le silence."
              OK    : "Bertrand fixa ses mains sans répondre."
              OK    : "Ses yeux noisette, légèrement myopes, lui donnaient un air rêveur."

            3. ARTEFACT DE SCÉNARIO
            Mots ou tournures qui appartiennent au script de fabrication, pas à la fiction :
            un fragment de consigne recopié dans la prose, ou une confirmation qu'une
            instruction a été suivie ("comme demandé", "conformément au plan", etc.).
              RÈGLE : la phrase doit être impossible sous la plume d'un romancier humain qui écrit librement.
              Observer le rythme ou la dynamique d'une scène n'est pas un artefact.
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

            Si tu détectes des fuites :
            FUITE
            - "[citation exacte, courte]" → type [1-5] — [une ligne d'explication]

            Si le texte est propre :
            OK

            Sois précis et sélectif. En français.""";

    private static final String AGENT_NAME = "SequenceDeusInMachinaCritic";

    private final ModelCallPort llm;

    @Override
    public String agentName() { return AGENT_NAME; }

    public DeusInMachinaCritic(ModelCallPort llm) {
        this.llm = llm;
    }

    public DeusInMachinaCriticOutput call(DeusInMachinaCriticInput input) {
        String user = PromptBuilder.create()
                .section("Consigne de séquence", input.sequenceDirective())
                .section("Plan de séquence",     input.sequencePlan())
                .raw(input.checks() == null || input.checks().isBlank()
                        ? "" : "Points de vérification actifs (pour référence) :\n" + input.checks())
                .raw("Texte à analyser :\n" + input.text())
                .raw("Réponds FUITE (avec liste) ou OK.")
                .build();
        String raw = llm.generate(SYSTEM, user, 0.2, LlmCallContext.of(agentName(), agentLabel()).withThink(thinks())).text().trim();
        return new DeusInMachinaCriticOutput(parseLeaks(raw));
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
