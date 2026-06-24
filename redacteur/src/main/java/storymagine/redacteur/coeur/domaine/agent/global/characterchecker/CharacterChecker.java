package storymagine.redacteur.coeur.domaine.agent.global.characterchecker;

import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.redacteur.coeur.domaine.agent.Agent;
import storymagine.redacteur.coeur.domaine.agent.commun.ProblemScoreParser;

import java.util.List;

/**
 * Checks character coherence against their GÉNÉRAL sheets and internal state continuity.
 * Only flags issues that would break credibility for an attentive reader.
 * Source: CharacterCheckerContext.check.
 */
public class CharacterChecker implements Agent {

    private static final String SYSTEM = """
        Tu es un éditeur de cohérence narrative. Tu identifies deux types d'incohérences :
        1. Contradictions avec la fiche personnage : le personnage agit, parle ou réagit
           d'une façon incompatible avec ce que sa fiche dit de lui (tempérament, traits
           physiques dominants, façon de s'exprimer).
        2. Ruptures d'état sans explication dans le chapitre : une blessure qui disparaît,
           un objet posé qui réapparaît en main, une tenue qui change sans transition narrative,
           une position physique impossible par rapport à la scène précédente.
        Ignore les petites imprécisions stylistiques et les ellipses narratives normales.
        Ne signale que les incohérences qui briseraient la crédibilité pour un lecteur attentif.
        Format de sortie strict :
        INCOHERENCE: [personnage — type (fiche / état) — description précise]
        SCORE: N  (entier 0-10, 10 = aucune incohérence ; chaque incohérence grave enlève 2-3 pts, mineure 1 pt)
        S'il n'y a aucune incohérence, écris uniquement : SCORE: 10
        En français.""";

    private static final String AGENT_NAME = "CharacterChecker";

    private final ModelCallPort llm;

    @Override
    public String agentName() { return AGENT_NAME; }

    public CharacterChecker(ModelCallPort llm) {
        this.llm = llm;
    }

    public CharacterCheckerOutput call(CharacterCheckerInput input) {
        String fichesBlock = (input.charactersText() == null || input.charactersText().isBlank())
            ? "(aucune fiche disponible — vérification d'état uniquement)"
            : input.charactersText();
        String user = "### Fiches personnages (section GÉNÉRAL)\n" + fichesBlock
            + "\n\n### Texte du chapitre\n" + (input.text() != null ? input.text() : "")
            + "\n\nIdentifie les incohérences. Conclus par SCORE: N.";
        String raw = llm.generate(SYSTEM, user, 0.2, LlmCallContext.of(agentName())).text();
        List<String> issues = ProblemScoreParser.parseTagged(raw, "INCOHERENCE");
        int score = ProblemScoreParser.parseScoreInt(raw);
        return new CharacterCheckerOutput(issues, score);
    }
}
