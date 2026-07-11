package storymagine.redacteur.coeur.domaine.agent.sequence.grammarcorrector;

import storymagine.commun.coeur.domaine.prompt.PromptBuilder;
import storymagine.commun.coeur.domaine.text.TruncHelper;
import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.LogPort;
import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.redacteur.coeur.domaine.agent.AgentCorrector;
import storymagine.redacteur.coeur.domaine.agent.commun.CorrectionParser;
import storymagine.redacteur.coeur.domaine.agent.commun.RetryStrategy;

import java.util.List;

/**
 * Detects grammar, spelling, agreement and conjugation errors in a sequence and returns
 * (wrong phrase, corrected phrase) pairs. Word choice and phrasing are out of scope — see
 * PhrasingCorrector. Corrections are applied in Java by the service layer — no second LLM call.
 */
public class GrammarCorrector implements AgentCorrector {

    private static final String SYSTEM = """
        Tu analyses ce texte et identifies UNIQUEMENT les erreurs mécaniques de la langue française :
        grammaire, orthographe, accord, conjugaison — des erreurs vérifiables par une règle précise,
        indépendamment du sens de la phrase.
        Ne change jamais le sens de la phrase ni les faits qu'elle rapporte.

        1. GRAMMAIRE : construction syntaxique qui viole une règle.
           FAUX: "C'est la maison que je te parlais."
           JUSTE: "C'est la maison dont je te parlais."
        2. ORTHOGRAPHE : mot mal orthographié.
           FAUX: "Elle marchait vers le chatau en ruine."
           JUSTE: "Elle marchait vers le château en ruine."
        3. ACCORD : accord en genre/nombre incorrect (adjectif, participe passé...).
           FAUX: "Les fleurs fanée jonchaient le sol."
           JUSTE: "Les fleurs fanées jonchaient le sol."
        4. CONJUGAISON : temps ou personne du verbe incorrect.
           FAUX: "Hier, il prend son épée et partit."
           JUSTE: "Hier, il prit son épée et partit."

        En cas de doute, ne signale rien.

        Format STRICT —
        Pour chaque problème :
        CORRECTIONS:
        - FAUX: "phrase exacte contenant le problème"
          JUSTE: "phrase corrigée"

        FAUX doit être recopié mot pour mot, en entier — jamais de "..." pour abréger un passage long.
        Ni FAUX ni JUSTE ne portent de commentaire ou de justification après la citation.

        ATTENTION : une seule phrase par ligne JUSTE, sans variante, sans commentaire, sans explication.

        Si aucun problème : retourner "PAS D'ERREUR" — rien avant, rien après.""";

    private static final String AGENT_NAME = "SequenceGrammarCorrector";

    private final ModelCallPort llm;
    private final LogPort       log;

    @Override
    public String agentName() { return AGENT_NAME; }

    @Override
    public RetryStrategy retryStrategy() { return RetryStrategy.DECREASING_AND_RATIO_THRESHOLD; }

    @Override
    public boolean thinks() { return false; }

    public GrammarCorrector(ModelCallPort llm, LogPort log) {
        this.llm = llm;
        this.log = log;
    }

    public GrammarCorrectorOutput call(GrammarCorrectorInput input) {
        int maxChars = llm.contextWindow() * 4 / 3;
        TruncHelper t = TruncHelper.create();
        String user  = PromptBuilder.create()
                .section("Texte", t.text(input.text(), maxChars, "text"))
                .raw("Analyse les fautes.")
                .build();
        t.logIfTruncated(log, agentName());
        String raw   = llm.generate(SYSTEM, user, 0.1,
                LlmCallContext.of(agentName(), agentLabel()).withThink(thinks())).text();
        return new GrammarCorrectorOutput(parseCorrections(raw));
    }

    private List<GrammarCorrectorOutput.Correction> parseCorrections(String response) {
        return CorrectionParser.parse(response, "PAS D'ERREUR", GrammarCorrectorOutput.Correction::new);
    }
}
