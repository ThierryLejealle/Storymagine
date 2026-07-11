package storymagine.redacteur.coeur.domaine.agent.plan.coherencecritic;

import storymagine.commun.coeur.domaine.prompt.PromptBuilder;
import storymagine.commun.coeur.domaine.text.TruncHelper;
import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.LogPort;
import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.redacteur.coeur.domaine.agent.Agent;
import storymagine.redacteur.coeur.domaine.agent.commun.CriticOutputParser;

/**
 * Checks factual coherence of a chapter plan (checks, constraints, focus).
 * Plan-phase equivalent of ChapterCoherenceCritic.
 * Source: CriticContext.evalPlanCoherence.
 */
public class PlanCoherenceCritic implements Agent {

    private static final String SYSTEM =
"""
Tu es un verificateur de coherence de plan.

La CONSIGNE DE L'AUTEUR (description) et l'OBJECTIF de ce chapitre te sont donnés avant le plan : ils font
foi et priment sur tout le reste — fiches personnage, contraintes, état des entités déjà établi. Tout élément
du plan qui en découle directement n'est JAMAIS une incohérence, même s'il contredit une fiche personnage ou
un fait établi ailleurs.

Au-delà de la consigne, tu analyses uniquement le plan selon les points suivants :
- les fiches personnage ;
- les contraintes additionnelles ;
- la continuité factuelle entre les séquences du plan et avec les faits déjà établis dans l'histoire (état des entités).

Ton objectif est de relever toutes les incohérences ou erreurs factuelles concernant ces points dans le PLAN fourni.
N'invente jamais d'incoherence si tout est correct.

Tu n'evalues PAS la progression narrative, la qualite litteraire, la grammaire, la syntaxe ni le style.

REGLE DE PORTEE : Le plan JSON peut contenir un champ "points_a_verifier" a l'interieur de chaque objet sequence. Ces points s'appliquent UNIQUEMENT a la sequence qui les contient — un point de S1 n'est pas valide pour S2 ou S3.
PROCEDURE OBLIGATOIRE :
1. Lis le plan, les fiches personnage et les contraintes additionelles et releve toutes les incoherences meme mineures. Pour chaque sequence, applique uniquement les contraintes propres a cette sequence, en plus des contraintes additionelles globales.
2. Qualifie chaque point :
   AMELIORATION: un detail factuel (type de materiel, rang, toponyme, date, trait physique) pourrait etre plus precis ou plus conforme a la fiche.
   Exemple : La S3 mentionne des Hurricanes en Afrique du Nord — la fiche de Pierre precise Spitfire Mk V ; verifier si c'est une erreur ou un autre pilote.
   Exemple : La S4 decrit Lea comme rousse alors que sa fiche la decrit brune.
   Si une sequence contient une information qui contredit partiellement un fait etabli, une contrainte, ou le comportement attendu d'un personnage selon sa fiche, c'est un DEFAUT_SIGNIFICATIF.
   Exemple : La S2 mentionne que Jules a ete stationne a Biggin Hill en 1942, or sa fiche indique qu'il n'a rejoint l'escadrille qu'en 1943.
   Exemple : La S3 decrit Eddie tenant un cafe — aucun cafe n'a ete etabli dans les sequences precedentes du plan.
   Si une sequence contredit un fait deja etabli dans l'histoire (section "Etat des entites"), c'est aussi un DEFAUT_MAJEUR.
   Exemple : L'etat des entites indique que Marc a la jambe cassee depuis le chapitre 2 ; la S2 le fait courir sans aucune allusion a cette blessure.
   Si une sequence contredit directement une contrainte explicite ou un fait fondamental du recit, c'est un DEFAUT_MAJEUR.
   Exemple : La S2 annonce la mission Gold Beach et les coordonnees radio — viole la contrainte : le premier briefing ne mentionne pas encore de mission specifique.
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
    private final LogPort       log;

    @Override
    public String agentName() { return AGENT_NAME; }

    public PlanCoherenceCritic(ModelCallPort llm, LogPort log) {
        this.llm = llm;
        this.log = log;
    }

    public PlanCoherenceCriticOutput call(PlanCoherenceCriticInput input) {
        int ctx = llm.contextWindow();
        TruncHelper t = TruncHelper.create();
        String chapterGoal  = t.text(input.chapterGoal(), 800, "chapterGoal");
        String characters   = t.blockList(input.characters(),  ctx * 4 / 8, "characters");
        String entityState  = t.list(input.entityState(), ctx * 4 / 12, "entityState");
        String checks       = t.list(input.checks(),      ctx * 4 / 8, "checks");

        String description = t.text(input.description(), ctx * 4 / 12, "description");

        String user = PromptBuilder.create()
            .section("Consigne de l'auteur (ce chapitre)", description)
            .section("Objectif du chapitre", chapterGoal)
            .section("Plan du chapitre", t.text(input.plan(), ctx * 4 * 55 / 100, "plan"))
            .section("Fiches personnage", characters)
            .section("État des entités (faits déjà établis)", entityState)
            .section("Points à vérifier", checks.isBlank() ? "" : "Vérifie que chacun des points suivants est respecté :\n" + checks)
            .build();
        t.logIfTruncated(log, agentName());

        String raw = llm.generate(SYSTEM, user, 0.3, LlmCallContext.of(agentName(), agentLabel()).withThink(thinks())).text();
        return new PlanCoherenceCriticOutput(CriticOutputParser.parseProblems(raw), CriticOutputParser.calculateScore(raw));
    }
}
