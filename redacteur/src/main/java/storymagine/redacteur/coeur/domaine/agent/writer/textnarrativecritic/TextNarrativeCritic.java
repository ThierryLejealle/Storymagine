package storymagine.redacteur.coeur.domaine.agent.writer.textnarrativecritic;

import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.redacteur.coeur.domaine.agent.Agent;
import storymagine.redacteur.coeur.domaine.agent.commun.CriticOutputParser;

/**
 * Evaluates the narrative arc progression of a finished chapter text.
 * Uses tiered output (AMELIORATION / DEFAUT_SIGNIFICATIF / DEFAUT_MAJEUR); score is derived.
 * Source: CriticContext.evalNarrative.
 */
public class TextNarrativeCritic implements Agent {

    private static final String SYSTEM = """
            Tu es un editeur narratif. Tu evalues tres soigneusement le TEXTE d'un chapitre.
            Tu verifies point par point tous les passages et elements du texte, en te focalisant
            exclusivement sur la progression de l'arc narratif : ton objectif est de lister
            tous les problemes, defauts ou faiblesses par rapport a la progression de l'arc narratif.
            Mais ne te force pas a inventer un defaut ou une amelioration si tout est correct.
            Tu n'evalues PAS la coherence factuelle.
            Si l'objectif du chapitre est fourni, tout element qui en decoule directement
            n'est pas un defaut — ne le signale pas.

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

    private static final String AGENT_NAME = "TextNarrativeCritic";

    private final ModelCallPort llm;

    @Override
    public String agentName() { return AGENT_NAME; }

    public TextNarrativeCritic(ModelCallPort llm) {
        this.llm = llm;
    }

    public TextNarrativeCriticOutput call(TextNarrativeCriticInput input) {
        int ctx = llm.contextWindow();
        String user = "### Texte\n"             + trunc(input.text(),    ctx * 4 * 55 / 100)
            + "\n\n### Objectif du chapitre\n"  + trunc(input.bookGoal(), ctx * 4 / 8)
            + "\n\nEvalue le texte.";
        String raw = llm.generate(SYSTEM, user, 0.3, LlmCallContext.of(agentName())).text();
        return new TextNarrativeCriticOutput(CriticOutputParser.parseProblems(raw), CriticOutputParser.calculateScore(raw));
    }

    private static String trunc(String s, int maxChars) {
        if (s == null || s.isBlank()) return "";
        return s.length() <= maxChars ? s : s.substring(0, maxChars) + "…";
    }
}
