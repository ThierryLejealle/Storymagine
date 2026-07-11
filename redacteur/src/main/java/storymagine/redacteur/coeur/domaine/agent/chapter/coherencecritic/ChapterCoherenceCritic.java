package storymagine.redacteur.coeur.domaine.agent.chapter.coherencecritic;

import storymagine.commun.coeur.domaine.prompt.PromptBuilder;
import storymagine.commun.coeur.domaine.text.TruncHelper;
import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.LogPort;
import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.redacteur.coeur.domaine.agent.Agent;
import storymagine.redacteur.coeur.domaine.agent.commun.CriticOutputParser;

/**
 * Checks factual coherence of a chapter text (checks, character sheets, constraints).
 * Does not evaluate narrative quality or style.
 * Source: CriticContext.evalCoherence.
 */
public class ChapterCoherenceCritic implements Agent {

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
            AMELIORATION : une ligne par amelioration. Si aucune, ecrire exactement : AMELIORATION : [RIEN]
            DEFAUT_SIGNIFICATIF : une ligne par defaut. Si aucun, ecrire exactement : DEFAUT_SIGNIFICATIF : [RIEN]
            DEFAUT_MAJEUR : une ligne par defaut. Si aucun, ecrire exactement : DEFAUT_MAJEUR : [RIEN]
            Interdit : ajouter du texte apres [RIEN], ou melanger [RIEN] avec une observation reelle.
            Rien d'autre : ni texte avant ni texte apres ces trois sections.
            Exemple - aucun probleme :
            AMELIORATION : [RIEN]
            DEFAUT_SIGNIFICATIF : [RIEN]
            DEFAUT_MAJEUR : [RIEN]
            En francais.""";

    private static final String AGENT_NAME = "ChapterCoherenceCritic";

    private final ModelCallPort llm;
    private final LogPort       log;

    @Override
    public String agentName() { return AGENT_NAME; }

    public ChapterCoherenceCritic(ModelCallPort llm, LogPort log) {
        this.llm = llm;
        this.log = log;
    }

    public ChapterCoherenceCriticOutput call(ChapterCoherenceCriticInput input) {
        int ctx = llm.contextWindow();
        TruncHelper t = TruncHelper.create();
        String user = PromptBuilder.create()
            .section("Texte", t.text(input.text(), ctx * 4 * 55 / 100, "text"))
            .section("Points à vérifier", "Vérifie que chacun des points suivants est respecté :\n" + t.list(input.checks(), ctx * 4 / 10, "checks"))
            .raw("Evalue la coherence du texte.")
            .build();
        t.logIfTruncated(log, agentName());
        String raw = llm.generate(SYSTEM, user, 0.3, LlmCallContext.of(agentName(), agentLabel()).withThink(thinks())).text();
        return new ChapterCoherenceCriticOutput(CriticOutputParser.parseProblems(raw), CriticOutputParser.calculateScore(raw));
    }
}
