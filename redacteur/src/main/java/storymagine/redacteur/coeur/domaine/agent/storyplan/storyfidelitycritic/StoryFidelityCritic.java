package storymagine.redacteur.coeur.domaine.agent.storyplan.storyfidelitycritic;

import storymagine.commun.coeur.domaine.prompt.PromptBuilder;
import storymagine.commun.coeur.domaine.text.TruncHelper;
import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.LogPort;
import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.redacteur.coeur.domaine.agent.Agent;
import storymagine.redacteur.coeur.domaine.agent.commun.CriticOutputParser;

/**
 * Verifies that each chapter plan faithfully realises the author's brief (description + goal),
 * across the whole book. Never judges the brief itself — only whether the LLM-generated plan
 * omits, inverts, or contradicts what it explicitly asked for.
 */
public class StoryFidelityCritic implements Agent {

    private static final String SYSTEM = """
            Tu es un editeur qui verifie la fidelite des PLANS de chapitre aux consignes de l'auteur.
            Pour chaque chapitre, la CONSIGNE DE L'AUTEUR (description, objectif) t'est donnee avant le plan genere par l'IA : elle fait foi. Le plan doit la mettre en scene par des beats, sans la trahir.
            Tu ne juges JAMAIS la qualite litteraire ni les choix narratifs de l'auteur — la consigne n'est jamais fautive. Tu juges uniquement si le PLAN respecte fidelement ce qu'elle demande.

            PROCEDURE OBLIGATOIRE :
            1. Pour chaque chapitre, lis d'abord sa consigne, puis verifie que le plan la met en scene sans en contredire ni en omettre un element important.
            2. Qualifie chaque ecart :
               - AMELIORATION: : le plan respecte la consigne mais un element explicite pourrait etre mieux exploite.
                 Exemple : La consigne du chapitre 3 mentionne la presence d'un ami, mais le plan ne le fait apparaitre que dans une seule sequence sur quatre.
               - Si le plan omet un element important explicitement demande par la consigne, ou inverse un fait precis (qui fait quoi, dans quel sens), c'est un DEFAUT_SIGNIFICATIF.
                 Exemple : La consigne precise que c'est Marc qui appelle Julie ; le plan montre Marc recevant l'appel — le sens de l'initiative est inverse.
               - Si le plan decrit un evenement oppose a celui demande par la consigne, c'est un DEFAUT_MAJEUR.
                 Exemple : La consigne du chapitre 4 demande une scene de depart marquant la fin d'un sejour ; le plan decrit une arrivee avec installation dans les lieux.

            FORMAT STRICT :
            AMELIORATION : avec une ligne par amelioration, ou [RIEN] si aucune.
            DEFAUT_SIGNIFICATIF : avec une ligne par defaut significatif, ou [RIEN] si aucun.
            DEFAUT_MAJEUR : avec une ligne par defaut majeur, ou [RIEN] si aucun.
            Rien d'autre : ni texte avant ni texte apres ces trois sections.

            Exemple 1 - un defaut majeur, rien d'autre :
            AMELIORATION : [RIEN]
            DEFAUT_SIGNIFICATIF : [RIEN]
            DEFAUT_MAJEUR : La consigne du chapitre 4 demande une scene de depart marquant la fin d'un sejour ; le plan decrit une arrivee avec installation dans les lieux.
            Exemple 2 - aucun probleme trouve :
            AMELIORATION : [RIEN]
            DEFAUT_SIGNIFICATIF : [RIEN]
            DEFAUT_MAJEUR : [RIEN]
            """;

    private static final String AGENT_NAME = "StoryFidelityCritic";

    private final ModelCallPort llm;
    private final LogPort       log;

    @Override
    public String agentName() { return AGENT_NAME; }

    public StoryFidelityCritic(ModelCallPort llm, LogPort log) {
        this.llm = llm;
        this.log = log;
    }

    public StoryFidelityCriticOutput call(StoryFidelityCriticInput input) {
        int ctx = llm.contextWindow();
        TruncHelper t = TruncHelper.create();
        String user = PromptBuilder.create()
                .section("Objectif du roman", input.bookGoal())
                .section("Chapitres : consigne de l'auteur puis plan genere",
                        t.text(input.chaptersBlock(), ctx * 4 / 3, "chaptersBlock"))
                .raw("Verifie que chaque plan respecte fidelement sa consigne.")
                .build();
        t.logIfTruncated(log, agentName());
        String raw = llm.generate(SYSTEM, user, 0.2, LlmCallContext.of(agentName(), agentLabel()).withThink(thinks())).text();
        return new StoryFidelityCriticOutput(CriticOutputParser.parseProblems(raw), CriticOutputParser.calculateScore(raw));
    }
}
