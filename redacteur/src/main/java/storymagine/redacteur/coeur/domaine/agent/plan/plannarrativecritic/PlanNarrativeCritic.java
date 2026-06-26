package storymagine.redacteur.coeur.domaine.agent.plan.plannarrativecritic;

import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.redacteur.coeur.domaine.agent.Agent;
import storymagine.redacteur.coeur.domaine.agent.commun.CriticOutputParser;

/**
 * Evaluates the narrative arc of a chapter plan (AMELIORATION/DEFAUT tiers).
 * Plan-phase equivalent of TextNarrativeCritic.
 * Source: CriticContext.evalPlanNarrative.
 */
public class PlanNarrativeCritic implements Agent {

    private static final String SYSTEM =
"""
Tu es un editeur narratif. Tu evalues tres soigneusement le PLAN d'un chapitre, pas le texte final.
Tu verifies point par point tous les elements et aspects du plan, en te focalisant exclusivement sur la progression de l'arc narratif : ton objectif est de lister tous les problemes, defauts ou faiblesses par rapport a la progression de l'arc narratif. Mais ne te force pas a inventer un defaut ou une amelioration si tout est correct.
Si l'objectif de ce chapitre est fourni, tout element du plan qui en decoule directement n'est pas un defaut — ne le signale pas.

PROCEDURE OBLIGATOIRE :
1. Lis attentivement le plan et l'objectif du chapitre et trouve tous les defauts meme mineurs par rapport a l'arc narratif.
2. Qualifie chaque point selon ces definitions :
   AMELIORATION: point qui pourrait etre affine ; la faiblesse, si elle existe, est quasi imperceptible et n'impacte pas l'arc.
   Exemple : La S1 pose bien l'atmosphere ; une image plus evocatrice renforcerait encore l'immersion sans changer l'arc.
   Si la faiblesse est plus marquee et affaiblit une sequence entiere ou brise un lien narratif entre sequences, sans contredire l'objectif, c'est un DEFAUT_SIGNIFICATIF.
   Exemple : Le lien entre la tension du Debarquement (S1) et l'insomnie de Pierre (S4) est absent — la progression thematique de l'arc est interrompue.
   Si un element CONTREDIT DIRECTEMENT l'objectif du chapitre, meme si le reste du plan est bien construit, c'est un DEFAUT_MAJEUR.
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

    @Override
    public String agentName() { return AGENT_NAME; }

    public PlanNarrativeCritic(ModelCallPort llm) {
        this.llm = llm;
    }

    public PlanNarrativeCriticOutput call(PlanNarrativeCriticInput input) {
        int ctx = llm.contextWindow();
        String user = "### Plan du chapitre\n"        + trunc(input.plan(),        ctx * 4 * 55 / 100)
            + "\n\n### Objectif de ce chapitre\n"    + trunc(input.chapterGoal(), 800)
            + "\n\n### Objectif du livre\n"           + trunc(input.bookGoal(),    ctx * 4 / 8)
            + "\n\nEvalue le plan.";
        String raw = llm.generate(SYSTEM, user, 0.3, LlmCallContext.of(agentName(), agentLabel())).text();
        return new PlanNarrativeCriticOutput(CriticOutputParser.parseProblems(raw), CriticOutputParser.calculateScore(raw));
    }

    private static String trunc(String s, int maxChars) {
        if (s == null || s.isBlank()) return "";
        return s.length() <= maxChars ? s : s.substring(0, maxChars) + "…";
    }
}
