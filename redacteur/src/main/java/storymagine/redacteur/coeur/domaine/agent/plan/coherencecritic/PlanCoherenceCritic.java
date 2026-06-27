package storymagine.redacteur.coeur.domaine.agent.plan.coherencecritic;

import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.redacteur.coeur.domaine.agent.Agent;
import storymagine.redacteur.coeur.domaine.agent.commun.CriticOutputParser;

/**
 * Checks factual coherence of a chapter plan (checks, constraints, focus).
 * Plan-phase equivalent of TextCoherenceCritic.
 * Source: CriticContext.evalPlanCoherence.
 */
public class PlanCoherenceCritic implements Agent {

    private static final String SYSTEM =
"""
Tu es un verificateur de coherence. Tu evalues tres soigneusement le PLAN d'un chapitre.
Tu verifies point par point tous les elements et aspects du plan : ton objectif est de relever toutes les incoherences, erreurs factuelles ou violations de contraintes, meme mineures. Mais ne te force pas a inventer une incoherence si tout est correct.
Tu verifies UNIQUEMENT : faits etablis, contraintes explicites (checks), fiches personnage (faits et psychologie des personnages), continuite factuelle.
Tu n'evalues PAS la progression narrative, la qualite litteraire, la grammaire, la syntaxe ni le style.
Si l'objectif du chapitre est fourni, tout element qui en decoule directement n'est pas une incoherence — ne le signale pas.

REGLE DE PORTEE : Le plan JSON peut contenir des champs "checks", "contraintes", "focus" et "lore" a l'interieur de chaque objet sequence. Ces champs s'appliquent UNIQUEMENT a la sequence qui les contient — un "checks" de S1 n'est pas valide pour S2 ou S3. Seuls les elements des sections globales du prompt ("Points a verifier", "Contraintes", "Éléments à utiliser") s'appliquent a l'ensemble du plan.

PROCEDURE OBLIGATOIRE :
1. Lis le plan, les checks et les fiches personnage et releve toutes les incoherences meme mineures. Pour chaque sequence, applique uniquement les checks et contraintes propres a cette sequence.
2. Qualifie chaque point :
   AMELIORATION: un detail factuel (type de materiel, rang, toponyme, date, trait physique) pourrait etre plus precis ou plus conforme a la fiche.
   Exemple : La S3 mentionne des Hurricanes en Afrique du Nord — la fiche de Pierre precise Spitfire Mk V ; verifier si c'est une erreur ou un autre pilote.
   Exemple : La S4 decrit Lea comme rousse alors que sa fiche la decrit brune.
   Si une sequence contient une information qui contredit partiellement un fait etabli, un check, ou le comportement attendu d'un personnage selon sa fiche, c'est un DEFAUT_SIGNIFICATIF.
   Exemple : La S2 mentionne que Jules a ete stationne a Biggin Hill en 1942, or sa fiche indique qu'il n'a rejoint l'escadrille qu'en 1943.
   Exemple : La S3 decrit Eddie tenant un cafe — aucun cafe n'a ete etabli dans les sequences precedentes du plan.
   Si une sequence contredit directement un check explicite ou un fait fondamental du recit, c'est un DEFAUT_MAJEUR.
   Exemple : La S2 annonce la mission Gold Beach et les coordonnees radio — viole le check : le premier briefing ne mentionne pas encore de mission specifique.
   Exemple : La S4 sort du compartiment — viole la contrainte : l'histoire ne quitte jamais le compartiment du train.

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

    private static final String AGENT_NAME = "PlanCoherenceCritic";

    private final ModelCallPort llm;

    @Override
    public String agentName() { return AGENT_NAME; }

    public PlanCoherenceCritic(ModelCallPort llm) {
        this.llm = llm;
    }

    public PlanCoherenceCriticOutput call(PlanCoherenceCriticInput input) {
        int ctx = llm.contextWindow();
        String chapterGoal = trunc(input.chapterGoal(), 800);
        String characters  = trunc(input.characters(),  ctx * 4 / 8);
        String checks      = trunc(input.checks(),      ctx * 4 / 8);
        String constraints = trunc(input.constraints(), ctx * 4 / 8);
        String focusText   = trunc(input.focusText(),   ctx * 4 / 8);
        String user = "### Plan du chapitre\n"                         + trunc(input.plan(), ctx * 4 * 55 / 100)
            + (chapterGoal.isBlank() ? "" : "\n\n### Objectif de ce chapitre\n"     + chapterGoal)
            + (characters.isBlank()  ? "" : "\n\n### Fiches personnage\n"           + characters)
            + (checks.isBlank()      ? "" : "\n\n### Points a verifier\n"           + checks)
            + (constraints.isBlank() ? "" : "\n\n### Contraintes\n"                 + constraints)
            + (focusText.isBlank()   ? "" : "\n\n### Éléments à utiliser (focus)\n" + focusText);
        String raw = llm.generate(SYSTEM, user, 0.3, LlmCallContext.of(agentName(), agentLabel())).text();
        return new PlanCoherenceCriticOutput(CriticOutputParser.parseProblems(raw), CriticOutputParser.calculateScore(raw));
    }

    private static String trunc(String s, int maxChars) {
        if (s == null || s.isBlank()) return "";
        return s.length() <= maxChars ? s : s.substring(0, maxChars) + "…";
    }
}
