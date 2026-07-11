package storymagine.redacteur.coeur.domaine.agent.plan.narrativecritic;

import storymagine.commun.coeur.domaine.prompt.PromptBuilder;
import storymagine.commun.coeur.domaine.text.TruncHelper;
import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.LogPort;
import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.redacteur.coeur.domaine.agent.Agent;
import storymagine.redacteur.coeur.domaine.agent.commun.CriticOutputParser;

/**
 * Evaluates the narrative arc of a chapter plan (AMELIORATION/DEFAUT tiers).
 * Plan-phase equivalent of ChapterNarrativeCritic.
 * Source: CriticContext.evalPlanNarrative.
 */
public class PlanNarrativeCritic implements Agent {

    private static final String SYSTEM =
"""
Tu es un editeur narratif. Tu evalues tres soigneusement le PLAN d'un chapitre, pas le texte final.
Tu verifies point par point tous les elements et aspects du plan, en te focalisant exclusivement sur la progression de l'arc narratif : ton objectif est de lister tous les problemes, defauts ou faiblesses par rapport a la progression de l'arc narratif. Mais ne te force pas a inventer un defaut ou une amelioration si tout est correct.
Si la consigne ou l'objectif de ce chapitre sont fournis, tout element du plan qui en decoule directement n'est pas un defaut — ne le signale pas.

PROCEDURE OBLIGATOIRE :
1. Lis attentivement le plan et l'objectif du chapitre et trouve tous les defauts meme mineurs par rapport a l'arc narratif.
2. Qualifie chaque point selon ces definitions :
   - AMELIORATION: : le plan atteint déjà correctement l'objectif du chapitre. Signale uniquement une faiblesse réelle mais très mineure de l'arc narratif. Ne propose jamais une autre manière de raconter la même histoire.
     Exemple : L’organisation des scènes est cohérente, mais l’équilibre entre exposition, développement et progression reste légèrement uniforme.
     Exemple : Transition légèrement rapide entre S1 et S2, ce qui réduit la perception progressive de la montée de tension.

   - Si la faiblesse est plus marquee et affaiblit une sequence entiere ou brise un lien narratif entre sequences, sans contredire l'objectif, c'est un DEFAUT_SIGNIFICATIF.
     Exemple : Le lien entre la tension du Debarquement (S1) et l'insomnie de Pierre (S4) est absent — la progression thematique de l'arc est interrompue.
   
   - Si un element CONTREDIT DIRECTEMENT l'objectif du chapitre, meme si le reste du plan est bien construit, c'est un DEFAUT_MAJEUR.
     Exemple : La S3 montre Pierre s'integrant chaleureusement dans l'escadrille, ce qui contredit directement l'objectif : pas de chaleur, pas d'integration.

FORMAT STRICT :
AMELIORATION : avec une ligne par amelioration, ou [RIEN] si aucune.
DEFAUT_SIGNIFICATIF : avec une ligne par defaut significatif, ou [RIEN] si aucun.
DEFAUT_MAJEUR : avec une ligne par defaut majeur, ou [RIEN] si aucun.
Rien d'autre : ni texte avant ni texte apres ces trois sections.

Exemple 1 - deux defauts significatifs, rien d'autre :
AMELIORATION : [RIEN]
DEFAUT_SIGNIFICATIF : Le heros n'a pas d'epee
DEFAUT_SIGNIFICATIF : l'ours est en peluche
DEFAUT_MAJEUR : [RIEN]
Exemple 2 - aucun probleme trouve (aucune amelioration, aucun defaut significatif, aucun defaut majeur) :
AMELIORATION : [RIEN]
DEFAUT_SIGNIFICATIF : [RIEN]
DEFAUT_MAJEUR : [RIEN]        
""";

    private static final String AGENT_NAME = "PlanNarrativeCritic";

    private final ModelCallPort llm;
    private final LogPort       log;

    @Override
    public String agentName() { return AGENT_NAME; }

    public PlanNarrativeCritic(ModelCallPort llm, LogPort log) {
        this.llm = llm;
        this.log = log;
    }

    public PlanNarrativeCriticOutput call(PlanNarrativeCriticInput input) {
        int ctx = llm.contextWindow();
        TruncHelper t = TruncHelper.create();
        String user = PromptBuilder.create()
            .section("Objectif du livre", t.text(input.bookGoal(), ctx * 4 / 8, "bookGoal"))
            .section("Consigne de l'auteur (ce chapitre)", t.text(input.chapterDescription(), ctx * 4 / 12, "chapterDescription"))
            .section("Objectif de ce chapitre", t.text(input.chapterGoal(), 800, "chapterGoal"))
            .section("Plan du chapitre", t.text(input.plan(), ctx * 4 * 55 / 100, "plan"))
            .raw("Evalue le plan.")
            .build();
        t.logIfTruncated(log, agentName());
        String raw = llm.generate(SYSTEM, user, 0.3, LlmCallContext.of(agentName(), agentLabel()).withThink(thinks())).text();
        return new PlanNarrativeCriticOutput(CriticOutputParser.parseProblems(raw), CriticOutputParser.calculateScore(raw));
    }
}
