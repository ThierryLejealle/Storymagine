package storymagine.redacteur.coeur.domaine.agent.chapter.narrativecritic;

import storymagine.commun.coeur.domaine.prompt.PromptBuilder;
import storymagine.commun.coeur.domaine.text.TruncHelper;
import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.LogPort;
import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.redacteur.coeur.domaine.agent.Agent;
import storymagine.redacteur.coeur.domaine.agent.commun.CriticOutputParser;

/**
 * Evaluates the narrative arc progression of a finished chapter text.
 * Uses tiered output (AMELIORATION / DEFAUT_SIGNIFICATIF / DEFAUT_MAJEUR); score is derived.
 * Source: CriticContext.evalNarrative.
 */
public class ChapterNarrativeCritic implements Agent {

    private static final String SYSTEM = """
            Tu es un editeur narratif. Tu evalues tres soigneusement le TEXTE d'un chapitre.
            Tu verifies point par point tous les passages et elements du texte, en te focalisant
            exclusivement sur la progression de l'arc narratif : ton objectif est de lister
            tous les problemes, defauts ou faiblesses par rapport a la progression de l'arc narratif.
            Mais ne te force pas a inventer un defaut ou une amelioration si tout est correct.
            Tu n'evalues PAS la coherence factuelle.
            Si la consigne ou l'objectif du chapitre sont fournis, tout element qui en decoule
            directement n'est pas un defaut — ne le signale pas.

            PROCEDURE OBLIGATOIRE :
            1. Lis attentivement le texte et l'objectif du chapitre et trouve tous les defauts
               meme mineurs par rapport a l'arc narratif.
            2. Qualifie chaque point :
               AMELIORATION: point qui pourrait etre affine ; la faiblesse est quasi imperceptible.
               DEFAUT_SIGNIFICATIF: faiblesse plus marquee qui affaiblit l'arc sans le contredire.
               DEFAUT_MAJEUR: element qui CONTREDIT DIRECTEMENT l'objectif du chapitre.
            Exemple de sortie :
            AMELIORATION : Le dialogue entre Pierre et Marie effleure leur tension passée sans l'approfondir — l'arc émotionnel aurait pu gagner en intensité.
            DEFAUT_SIGNIFICATIF : La montée en tension s'effondre à mi-chapitre : les deux derniers paragraphes relâchent la pression sans retournement.
            DEFAUT_MAJEUR : Pierre est décrit comme apaisé dès le premier paragraphe, ce qui contredit directement l'objectif — le chapitre devait montrer son rejet.

            FORMAT STRICT :
            AMELIORATION : avec une ligne par amelioration, ou [RIEN] si aucune.
            DEFAUT_SIGNIFICATIF : avec une ligne par defaut significatif, ou [RIEN] si aucun.
            DEFAUT_MAJEUR : avec une ligne par defaut majeur, ou [RIEN] si aucun.
            Rien d'autre : ni texte avant ni texte apres ces trois sections.
            En francais.""";

    private static final String AGENT_NAME = "ChapterNarrativeCritic";

    private final ModelCallPort llm;
    private final LogPort       log;

    @Override
    public String agentName() { return AGENT_NAME; }

    public ChapterNarrativeCritic(ModelCallPort llm, LogPort log) {
        this.llm = llm;
        this.log = log;
    }

    public ChapterNarrativeCriticOutput call(ChapterNarrativeCriticInput input) {
        int ctx = llm.contextWindow();
        TruncHelper t = TruncHelper.create();
        String user = PromptBuilder.create()
            .section("Consigne de l'auteur (ce chapitre)",  t.text(input.chapterDescription(), ctx * 4 / 12, "chapterDescription"))
            .section("Objectif narratif de ce chapitre",    input.chapterGoal())
            .section("Objectif global du roman (contexte)", t.text(input.bookGoal(), 1600, "bookGoal"))
            .section("Texte à évaluer",                     t.text(input.text(), ctx * 4 / 2, "text"))
            .raw("Evalue le texte.")
            .build();
        t.logIfTruncated(log, agentName());
        String raw = llm.generate(SYSTEM, user, 0.3, LlmCallContext.of(agentName(), agentLabel()).withThink(thinks())).text();
        return new ChapterNarrativeCriticOutput(CriticOutputParser.parseProblems(raw), CriticOutputParser.calculateScore(raw));
    }
}
