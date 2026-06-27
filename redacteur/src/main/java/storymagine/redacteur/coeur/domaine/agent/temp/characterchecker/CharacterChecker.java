package storymagine.redacteur.coeur.domaine.agent.temp.characterchecker;

import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.redacteur.coeur.domaine.agent.Agent;
import storymagine.redacteur.coeur.domaine.agent.commun.ProblemScoreParser;

import java.util.List;

/**
 * Checks character coherence against their GÃ‰NÃ‰RAL sheets and internal state continuity.
 * Only flags issues that would break credibility for an attentive reader.
 * Source: CharacterCheckerContext.check.
 */
public class CharacterChecker implements Agent {

    private static final String SYSTEM = """
        Tu es un Ã©diteur de cohÃ©rence narrative. Tu identifies deux types d'incohÃ©rences :
        1. Contradictions avec la fiche personnage : le personnage agit, parle ou rÃ©agit
           d'une faÃ§on incompatible avec ce que sa fiche dit de lui (tempÃ©rament, traits
           physiques dominants, faÃ§on de s'exprimer).
        2. Ruptures d'Ã©tat sans explication dans le chapitre : une blessure qui disparaÃ®t,
           un objet posÃ© qui rÃ©apparaÃ®t en main, une tenue qui change sans transition narrative,
           une position physique impossible par rapport Ã  la scÃ¨ne prÃ©cÃ©dente.
        Ignore les petites imprÃ©cisions stylistiques et les ellipses narratives normales.
        Ne signale que les incohÃ©rences qui briseraient la crÃ©dibilitÃ© pour un lecteur attentif.
        Format de sortie strict :
        INCOHERENCE: [personnage â€” type (fiche / Ã©tat) â€” description prÃ©cise]
        SCORE: N  (entier 0-10, 10 = aucune incohÃ©rence ; chaque incohÃ©rence grave enlÃ¨ve 2-3 pts, mineure 1 pt)
        S'il n'y a aucune incohÃ©rence, Ã©cris uniquement : SCORE: 10
        En franÃ§ais.""";

    private static final String AGENT_NAME = "CharacterChecker";

    private final ModelCallPort llm;

    @Override
    public String agentName() { return AGENT_NAME; }

    public CharacterChecker(ModelCallPort llm) {
        this.llm = llm;
    }

    public CharacterCheckerOutput call(CharacterCheckerInput input) {
        String fichesBlock = (input.charactersText() == null || input.charactersText().isBlank())
            ? "(aucune fiche disponible â€” vÃ©rification d'Ã©tat uniquement)"
            : input.charactersText();
        String user = "### Fiches personnages (section GÃ‰NÃ‰RAL)\n" + fichesBlock
            + "\n\n### Texte du chapitre\n" + (input.text() != null ? input.text() : "")
            + "\n\nIdentifie les incohÃ©rences. Conclus par SCORE: N.";
        String raw = llm.generate(SYSTEM, user, 0.2, LlmCallContext.of(agentName(), agentLabel())).text();
        List<String> issues = ProblemScoreParser.parseTagged(raw, "INCOHERENCE");
        int score = ProblemScoreParser.parseScoreInt(raw);
        return new CharacterCheckerOutput(issues, score);
    }
}
