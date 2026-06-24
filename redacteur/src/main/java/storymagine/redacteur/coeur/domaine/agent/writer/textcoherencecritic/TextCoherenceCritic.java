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

    private static final String SYSTEM =
        "Tu es un verificateur de coherence. Tu evalues tres soigneusement le TEXTE d'un chapitre.\n"
        + "Tu verifies point par point tous les passages et elements du texte : ton objectif est de"
        + " relever toutes les incoherences, erreurs factuelles ou violations de contraintes, meme mineures."
        + " Mais ne te force pas a inventer une incoherence si tout est correct.\n"
        + "Tu verifies UNIQUEMENT : faits etablis, contraintes explicites (checks),"
        + " fiches personnage (faits et psychologie des personnages), continuite factuelle dans le texte.\n"
        + "Tu n'evalues PAS la progression narrative, la qualite litteraire, la grammaire, la syntaxe ni le style.\n\n"
        + "PROCEDURE OBLIGATOIRE :\n"
        + "1. Lis le texte, les checks et les fiches personnage et releve toutes les incoherences meme mineures.\n"
        + "2. Qualifie chaque point :\n"
        + "   AMELIORATION: un detail factuel pourrait etre plus precis ou conforme a la fiche.\n"
        + "   DEFAUT_SIGNIFICATIF: information qui contredit partiellement un fait etabli ou un check.\n"
        + "   DEFAUT_MAJEUR: contradiction directe d'un check explicite ou d'un fait fondamental.\n"
        + "FORMAT STRICT :\n"
        + "AMELIORATION : avec une ligne par amelioration, ou [RIEN] si aucune.\n"
        + "DEFAUT_SIGNIFICATIF : avec une ligne par defaut significatif, ou [RIEN] si aucun.\n"
        + "DEFAUT_MAJEUR : avec une ligne par defaut majeur, ou [RIEN] si aucun.\n"
        + "Rien d'autre : ni texte avant ni texte apres ces trois lignes.\n"
        + "En francais.";

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
            + "\n\n### Éléments à utiliser (focus)\n"        + trunc(input.focusText(),   ctx * 4 / 10)
            + "\n\nEvalue la coherence du texte.";
        String raw = llm.generate(SYSTEM, user, 0.3, LlmCallContext.of(agentName())).text();
        return new TextCoherenceCriticOutput(CriticOutputParser.parseProblems(raw), CriticOutputParser.calculateScore(raw));
    }

    private static String trunc(String s, int maxChars) {
        if (s == null || s.isBlank()) return "";
        return s.length() <= maxChars ? s : s.substring(0, maxChars) + "…";
    }
}
