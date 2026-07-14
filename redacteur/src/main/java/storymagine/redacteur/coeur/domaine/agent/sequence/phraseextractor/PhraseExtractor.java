package storymagine.redacteur.coeur.domaine.agent.sequence.phraseextractor;

import storymagine.commun.coeur.domaine.prompt.PromptBuilder;
import storymagine.commun.coeur.domaine.text.TruncHelper;
import storymagine.commun.coeur.ports.GenerationOptions;
import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.LogPort;
import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.redacteur.coeur.domaine.agent.Agent;

/**
 * Relocates a Corrector's citation that failed to match the text verbatim (minor recopy error:
 * agreement, punctuation, moved word) by finding the actual matching passage in the source text.
 * Used as a rescue step when TextPatcher.apply() reports a miss — never called on its own pass.
 *
 * thinks() is false : the task is a closed, mechanical text search (find this exact wording, or
 * say so), not a judgment call reasoning improves — see Agent.thinks() javadoc. This also guards
 * against a real production failure (2026-07-13) : with reasoning enabled and no generation cap,
 * the model once spent 644s generating 32k tokens without ever emitting the expected one-line
 * "PASSAGE:"/"NOT_FOUND" answer, silently degrading to a NOT_FOUND indistinguishable from a
 * genuine one. maxTokens(300) below is the second line of defense, independent of why a future
 * degeneration might happen.
 */
public class PhraseExtractor implements Agent {

    private static final String SYSTEM = """
        Tu localises un passage dans un texte. On t'indique une citation qui ne correspond plus
        exactement au texte (petite erreur de recopie : accord, ponctuation, mot déplacé).
        Retrouve dans le texte le passage réellement visé et recopie-le mot pour mot, exactement
        comme il apparaît dans le texte.

        Format STRICT :
        PASSAGE: "passage recopié mot pour mot du texte"

        Si aucun passage suffisamment proche n'existe dans le texte : NOT_FOUND — rien d'autre.

        Exemple 1 — passage trouvé (la citation avait une erreur d'accord) :
        Citation : "Elle se repositiona pour le tir."
        Réponse :
        PASSAGE: "Elle se repositionna pour le tir."

        Exemple 2 — absent du texte :
        Citation : "Il traversa le pont en courant."
        Réponse :
        NOT_FOUND""";

    private static final String AGENT_NAME  = "SequencePhraseExtractor";
    private static final String QUOTE_CHARS = "\"'“”‘’";

    private final ModelCallPort llm;
    private final LogPort       log;

    @Override
    public String agentName() { return AGENT_NAME; }

    @Override
    public boolean thinks() { return false; }

    /** Belt-and-suspenders cap on top of thinks()=false — see class javadoc. */
    private static final int MAX_TOKENS = 300;

    public PhraseExtractor(ModelCallPort llm, LogPort log) {
        this.llm = llm;
        this.log = log;
    }

    public PhraseExtractorOutput call(PhraseExtractorInput input) {
        int maxChars = llm.contextWindow() * 4 / 3;
        TruncHelper t = TruncHelper.create();
        String user  = PromptBuilder.create()
                .section("Texte", t.text(input.text(), maxChars, "text"))
                .section("Citation qui ne correspond pas", "\"" + input.wrongPhrase() + "\"")
                .raw("Localise le passage réel.")
                .build();
        t.logIfTruncated(log, agentName());
        String raw = llm.generate(SYSTEM, user, 0.1,
                LlmCallContext.of(agentName(), agentLabel()).withThink(thinks()),
                GenerationOptions.maxTokens(MAX_TOKENS)).text();
        PhraseExtractorOutput output = parseResponse(raw);
        if (!output.found() && (raw == null || !raw.trim().startsWith("NOT_FOUND"))) {
            log.warn(agentName() + " : réponse inexploitable (ni PASSAGE: ni NOT_FOUND), traitée comme "
                    + "NOT_FOUND — " + (raw == null ? "réponse nulle" : raw.length() + " caractères reçus"));
        }
        return output;
    }

    static PhraseExtractorOutput parseResponse(String raw) {
        if (raw == null) return PhraseExtractorOutput.notFound();
        String trimmed = raw.trim();
        if (trimmed.startsWith("NOT_FOUND")) return PhraseExtractorOutput.notFound();
        for (String line : trimmed.split("\n")) {
            String t = line.trim();
            if (t.startsWith("PASSAGE:")) {
                String phrase = sanitize(t.substring("PASSAGE:".length()).trim());
                return phrase.isBlank() ? PhraseExtractorOutput.notFound() : new PhraseExtractorOutput(phrase, true);
            }
        }
        return PhraseExtractorOutput.notFound();
    }

    private static String sanitize(String s) {
        int start = 0;
        int end   = s.length();
        while (start < end && isQuote(s.charAt(start))) start++;
        while (end > start && isQuote(s.charAt(end - 1))) end--;
        return s.substring(start, end).trim();
    }

    private static boolean isQuote(char c) {
        return QUOTE_CHARS.indexOf(c) >= 0;
    }
}
