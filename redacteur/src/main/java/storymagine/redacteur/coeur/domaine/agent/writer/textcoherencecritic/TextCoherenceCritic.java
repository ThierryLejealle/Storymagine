package storymagine.redacteur.coeur.domaine.agent.writer.textcoherencecritic;

import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.redacteur.coeur.domaine.agent.Agent;
import storymagine.redacteur.coeur.domaine.agent.commun.CriticOutputParser;

/**
 * Checks factual coherence of a chapter text (checks, character sheets, constraints).
 * Does not evaluate narrative quality or style.
 * Source: CriticContext.evalCoherence.
 */
public class TextCoherenceCritic implements Agent {

    private static final String SYSTEM = """
            Tu es un verificateur de coherence. Tu evalues tres soigneusement le TEXTE d'un chapitre.
            Tu verifies point par point tous les passages et elements du texte : ton objectif est de
            relever toutes les incoherences, erreurs factuelles ou violations de contraintes, meme mineures.
            Mais ne te force pas a inventer une incoherence si tout est correct.
            Tu verifies UNIQUEMENT : faits etablis, contraintes explicites (checks),
            fiches personnage (faits et psychologie des personnages), continuite factuelle dans le texte.
            Tu n'evalues PAS la progression narrative, la qualite litteraire, la grammaire, la syntaxe ni le style.

            PROCEDURE OBLIGATOIRE :
            1. Lis le texte, les checks et les fiches personnage et releve toutes les incoherences meme mineures.
            2. Qualifie chaque point :
               AMELIORATION: un detail factuel pourrait etre plus precis ou conforme a la fiche.
               DEFAUT_SIGNIFICATIF: information qui contredit partiellement un fait etabli ou un check.
               DEFAUT_MAJEUR: contradiction directe d'un check explicite ou d'un fait fondamental.
            Exemple de sortie :
            AMELIORATION : Le texte décrit Marc comme "agité" ; sa fiche le dit "méticuleux et discret" — sans contradiction directe mais légèrement discordant.
            DEFAUT_SIGNIFICATIF : Pierre porte un manteau gris alors que sa fiche le décrit toujours en veston sombre.
            DEFAUT_MAJEUR : Marie apparaît dans la scène alors qu'un check précise qu'elle a quitté la ville le matin même.

            FORMAT STRICT :
            AMELIORATION : avec une ligne par amelioration, ou [RIEN] si aucune.
            DEFAUT_SIGNIFICATIF : avec une ligne par defaut significatif, ou [RIEN] si aucun.
            DEFAUT_MAJEUR : avec une ligne par defaut majeur, ou [RIEN] si aucun.
            Rien d'autre : ni texte avant ni texte apres ces trois sections.
            En francais.""";

    private static final String AGENT_NAME = "TextCoherenceCritic";

    private final ModelCallPort llm;

    @Override
    public String agentName() { return AGENT_NAME; }

    public TextCoherenceCritic(ModelCallPort llm) {
        this.llm = llm;
    }

    public TextCoherenceCriticOutput call(TextCoherenceCriticInput input) {
        int ctx = llm.contextWindow();
        String user = "### Texte\n"                          + trunc(input.text(),        ctx * 4 * 55 / 100)
            + "\n\n### Questions de coherence\n"            + trunc(input.checks(),      ctx * 4 / 10)
            + "\n\n### Contraintes\n"                       + trunc(input.constraints(), ctx * 4 / 10)
            + "\n\nEvalue la coherence du texte.";
        String raw = llm.generate(SYSTEM, user, 0.3, LlmCallContext.of(agentName(), agentLabel())).text();
        return new TextCoherenceCriticOutput(CriticOutputParser.parseProblems(raw), CriticOutputParser.calculateScore(raw));
    }

    private static String trunc(String s, int maxChars) {
        if (s == null || s.isBlank()) return "";
        return s.length() <= maxChars ? s : s.substring(0, maxChars) + "…";
    }
}
