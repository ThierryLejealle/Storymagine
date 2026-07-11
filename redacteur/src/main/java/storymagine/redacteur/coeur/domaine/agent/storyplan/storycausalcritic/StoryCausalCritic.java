package storymagine.redacteur.coeur.domaine.agent.storyplan.storycausalcritic;

import storymagine.commun.coeur.domaine.prompt.PromptBuilder;
import storymagine.commun.coeur.domaine.text.TruncHelper;
import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.LogPort;
import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.redacteur.coeur.domaine.agent.Agent;
import storymagine.redacteur.coeur.domaine.agent.commun.CriticOutputParser;

/**
 * Evaluates inter-chapter causal coherence across all chapter plans — book-level equivalent of
 * PlanCoherenceCritic. Only judges what the plan ADDS beyond the author's brief (description +
 * goal), given first for each chapter — the brief itself is never a defect.
 * Source: NarrativeCritiqueContext.evalCausal.
 */
public class StoryCausalCritic implements Agent {

    private static final String SYSTEM = """
            Tu es un verificateur de coherence causale. Tu evalues les PLANS de TOUS les chapitres d'un roman.
            Pour chaque chapitre, la CONSIGNE DE L'AUTEUR (description, objectif) t'est donnee avant le plan genere : elle fait foi et n'est JAMAIS fautive, meme si elle decrit un evenement soudain ou non explique.
            Tu verifies exclusivement les faits que le plan AJOUTE au-dela de la consigne : un detail, une cause, une consequence que l'IA a invente pour mettre en scene la consigne. Ton objectif est de detecter si ces ajouts de l'IA (jamais la consigne elle-meme) se contredisent factuellement d'un chapitre a l'autre. Mais ne te force pas a inventer un defaut si tout est correct.

            PROCEDURE OBLIGATOIRE :
            1. Pour chaque chapitre, repere ce que la consigne demande explicitement, puis ce que le plan y ajoute.
            2. Qualifie chaque probleme concernant UNIQUEMENT ces ajouts :
               - AMELIORATION: : un ajout de l'IA etablit un lien de cause a effet correct mais pourrait etre rendu plus explicite entre deux chapitres.
                 Exemple : Le plan du chapitre 3 ajoute une blessure au bras de Pierre qui explique sa lenteur au chapitre 5, mais le lien n'est jamais rappele explicitement.
               - Si deux ajouts de l'IA dans des chapitres differents se contredisent sur un fait, c'est un DEFAUT_SIGNIFICATIF.
                 Exemple : Le plan du chapitre 2 ajoute une voiture rouge pour Julien, mais le plan du chapitre 6 l'ajoute bleue sans explication.
               - Si un ajout de l'IA contredit directement un fait deja etabli par un autre ajout de l'IA dans un chapitre precedent, c'est un DEFAUT_MAJEUR.
                 Exemple : Le plan du chapitre 2 ajoute l'installation des personnages a l'hotel pour plusieurs jours ; le plan du chapitre 4 ajoute une nouvelle arrivee et un enregistrement a l'hotel, comme si rien ne s'etait passe depuis.

            FORMAT STRICT :
            AMELIORATION : avec une ligne par amelioration, ou [RIEN] si aucune.
            DEFAUT_SIGNIFICATIF : avec une ligne par defaut significatif, ou [RIEN] si aucun.
            DEFAUT_MAJEUR : avec une ligne par defaut majeur, ou [RIEN] si aucun.
            Rien d'autre : ni texte avant ni texte apres ces trois sections.

            Exemple 1 - deux defauts significatifs, rien d'autre :
            AMELIORATION : [RIEN]
            DEFAUT_SIGNIFICATIF : Le plan du chapitre 2 ajoute une voiture rouge pour Julien, mais le plan du chapitre 6 l'ajoute bleue sans explication.
            DEFAUT_SIGNIFICATIF : Le plan du chapitre 3 ajoute un chien pour Claire, jamais mentionne ensuite bien que le plan du chapitre 5 se deroule chez elle.
            DEFAUT_MAJEUR : [RIEN]
            Exemple 2 - aucun probleme trouve :
            AMELIORATION : [RIEN]
            DEFAUT_SIGNIFICATIF : [RIEN]
            DEFAUT_MAJEUR : [RIEN]
            """;

    private static final String AGENT_NAME = "StoryCausalCritic";

    private final ModelCallPort llm;
    private final LogPort       log;

    @Override
    public String agentName() { return AGENT_NAME; }

    public StoryCausalCritic(ModelCallPort llm, LogPort log) {
        this.llm = llm;
        this.log = log;
    }

    public StoryCausalCriticOutput call(StoryCausalCriticInput input) {
        int ctx = llm.contextWindow();
        TruncHelper t = TruncHelper.create();
        String user = PromptBuilder.create()
                .section("Objectif du roman", input.bookGoal())
                .section("Chapitres : consigne de l'auteur puis plan genere",
                        t.text(input.chaptersBlock(), ctx * 4 / 3, "chaptersBlock"))
                .raw("Evalue la coherence causale des faits ajoutes par le plan, au-dela des consignes.")
                .build();
        t.logIfTruncated(log, agentName());
        String raw = llm.generate(SYSTEM, user, 0.3, LlmCallContext.of(agentName(), agentLabel()).withThink(thinks())).text();
        return new StoryCausalCriticOutput(CriticOutputParser.parseProblems(raw), CriticOutputParser.calculateScore(raw));
    }
}
