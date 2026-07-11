package storymagine.redacteur.coeur.domaine.agent.storyplan.storynarrativecritic;

import storymagine.commun.coeur.domaine.prompt.PromptBuilder;
import storymagine.commun.coeur.domaine.text.TruncHelper;
import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.LogPort;
import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.redacteur.coeur.domaine.agent.Agent;
import storymagine.redacteur.coeur.domaine.agent.commun.CriticOutputParser;

/**
 * Evaluates character arcs across all chapter plans of the book — book-level equivalent of
 * PlanNarrativeCritic. Only judges what the plan ADDS beyond the author's brief (description +
 * goal), given first for each chapter — the brief itself is never a defect.
 * Source: NarrativeCritiqueContext.evalArcs.
 */
public class StoryNarrativeCritic implements Agent {

    private static final String SYSTEM = """
            Tu es un editeur narratif. Tu evalues les PLANS de TOUS les chapitres d'un roman.
            Pour chaque chapitre, la CONSIGNE DE L'AUTEUR (description, objectif) t'est donnee avant le plan genere : elle fait foi et n'est JAMAIS fautive, meme si elle decrit un depart, une disparition de personnage ou un evenement soudain.
            Tu verifies exclusivement les arcs de personnages que le plan AJOUTE au-dela de la consigne : details, gestes, sensations que l'IA a invente pour mettre en scene la consigne. Ton objectif est de detecter si ces ajouts de l'IA (jamais la consigne elle-meme) creent une incoherence d'arc entre chapitres : contradiction entre deux ajouts, ou ajout redondant d'un chapitre a l'autre. Mais ne te force pas a inventer un defaut si tout est correct.

            PROCEDURE OBLIGATOIRE :
            1. Pour chaque chapitre, repere ce que la consigne demande explicitement, puis ce que le plan y ajoute.
            2. Qualifie chaque probleme concernant UNIQUEMENT ces ajouts :
               - AMELIORATION: : un ajout de l'IA fonctionne mais pourrait etre mieux raccorde a un ajout d'un autre chapitre.
                 Exemple : Le plan du chapitre 3 ajoute un contact physique (main tenue) non demande par la consigne, qui anticipe legerement sur la prise de conscience amoureuse mise en scene au chapitre 4.
               - Si deux ajouts de l'IA dans des chapitres differents se contredisent sur l'arc d'un personnage, c'est un DEFAUT_SIGNIFICATIF.
                 Exemple : Le plan du chapitre 2 ajoute un trait de caractere timide pour Marc, mais le plan du chapitre 5 l'ajoute soudain audacieux sans transition, sans que la consigne ne demande ce changement.
               - Si un ajout de l'IA contredit directement un fait etabli par un autre ajout de l'IA dans un chapitre precedent, c'est un DEFAUT_MAJEUR.
                 Exemple : Le plan du chapitre 3 ajoute la disparition d'un bracelet porte par Claire, or le plan du chapitre 6 l'ajoute de nouveau a son poignet sans explication.

            FORMAT STRICT :
            AMELIORATION : avec une ligne par amelioration, ou [RIEN] si aucune.
            DEFAUT_SIGNIFICATIF : avec une ligne par defaut significatif, ou [RIEN] si aucun.
            DEFAUT_MAJEUR : avec une ligne par defaut majeur, ou [RIEN] si aucun.
            Rien d'autre : ni texte avant ni texte apres ces trois sections.

            Exemple 1 - deux defauts significatifs, rien d'autre :
            AMELIORATION : [RIEN]
            DEFAUT_SIGNIFICATIF : Le plan du chapitre 2 ajoute un trait de caractere timide pour Marc, mais le plan du chapitre 5 l'ajoute soudain audacieux sans transition.
            DEFAUT_SIGNIFICATIF : Le plan du chapitre 3 ajoute une amitie naissante entre Julie et Sophie, mais le plan du chapitre 4 les ajoute de nouveau comme etrangeres l'une a l'autre.
            DEFAUT_MAJEUR : [RIEN]
            Exemple 2 - aucun probleme trouve :
            AMELIORATION : [RIEN]
            DEFAUT_SIGNIFICATIF : [RIEN]
            DEFAUT_MAJEUR : [RIEN]
            """;

    private static final String AGENT_NAME = "StoryNarrativeCritic";

    private final ModelCallPort llm;
    private final LogPort       log;

    @Override
    public String agentName() { return AGENT_NAME; }

    public StoryNarrativeCritic(ModelCallPort llm, LogPort log) {
        this.llm = llm;
        this.log = log;
    }

    public StoryNarrativeCriticOutput call(StoryNarrativeCriticInput input) {
        int ctx = llm.contextWindow();
        TruncHelper t = TruncHelper.create();
        String user = PromptBuilder.create()
                .section("Objectif du roman", input.bookGoal())
                .section("Chapitres : consigne de l'auteur puis plan genere",
                        t.text(input.chaptersBlock(), ctx * 4 / 3, "chaptersBlock"))
                .raw("Evalue les arcs de personnages ajoutes par le plan, au-dela des consignes.")
                .build();
        t.logIfTruncated(log, agentName());
        String raw = llm.generate(SYSTEM, user, 0.3, LlmCallContext.of(agentName(), agentLabel()).withThink(thinks())).text();
        return new StoryNarrativeCriticOutput(CriticOutputParser.parseProblems(raw), CriticOutputParser.calculateScore(raw));
    }
}
