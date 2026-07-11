package storymagine.redacteur.coeur.domaine.agent.sequence.phrasingcorrector;

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
 * Detects residual LLM generation errors in a sequence — missing word, missing chunk, aberrant
 * word choice, non-idiomatic calque — and returns (wrong phrase, corrected phrase) pairs.
 * Grammar, spelling, agreement and conjugation are out of scope. Corrections are applied in
 * Java by the service layer — no second LLM call.
 */
public class PhrasingCorrector implements AgentCorrector {

    private static final String SYSTEM = """
        Tu détectes UNIQUEMENT des erreurs de génération résiduelles : des phrases qui sont clairement
        incorrectes parce qu'il manque un mot, un morceau, qu'un mot erroné a été utilisé, ou qu'une
        tournure calque une autre langue de façon non naturelle.
        Ne change jamais le sens de la phrase ni les faits qu'elle rapporte. Si un mot ou un morceau
        manque, complète en restant cohérent avec le reste du texte, en faisant au plus simple.

        1. MOT MANQUANT : la phrase est incompréhensible sans lui.
           FAUX: "Il ouvrit la et sortit."
           JUSTE: "Il ouvrit la porte et sortit."
        2. MOT ABERRANT : un mot rend la phrase absurde. Signale seulement si c'est évident.
           FAUX: "Elle caressa doucement le firmament du chat."
           JUSTE: "Elle caressa doucement le pelage du chat."
        3. PHRASE INTERROMPUE : la phrase s'arrête avant sa fin.
           FAUX: "Elle regarda par la fenêtre. Puis elle."
           JUSTE: "Elle regarda par la fenêtre. Puis elle se tut."
        4. CALQUE : tournure copiée d'une autre langue, non naturelle en français.
           FAUX: "Elle prit une décision de partir."
           JUSTE: "Elle décida de partir."

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

    private static final String AGENT_NAME = "SequencePhrasingCorrector";

    private final ModelCallPort llm;
    private final LogPort       log;

    @Override
    public String agentName() { return AGENT_NAME; }

    @Override
    public RetryStrategy retryStrategy() { return RetryStrategy.SINGLE_PASS; }

    @Override
    public boolean thinks() { return false; }

    public PhrasingCorrector(ModelCallPort llm, LogPort log) {
        this.llm = llm;
        this.log = log;
    }

    public PhrasingCorrectorOutput call(PhrasingCorrectorInput input) {
        int maxChars = llm.contextWindow() * 4 / 3;
        TruncHelper t = TruncHelper.create();
        String user  = PromptBuilder.create()
                .section("Texte", t.text(input.text(), maxChars, "text"))
                .raw("Analyse les erreurs de génération.")
                .build();
        t.logIfTruncated(log, agentName());
        String raw   = llm.generate(SYSTEM, user, 0.1,
                LlmCallContext.of(agentName(), agentLabel()).withThink(thinks())).text();
        return new PhrasingCorrectorOutput(parseCorrections(raw));
    }

    private List<PhrasingCorrectorOutput.Correction> parseCorrections(String response) {
        return CorrectionParser.parse(response, "PAS D'ERREUR", PhrasingCorrectorOutput.Correction::new);
    }
}
