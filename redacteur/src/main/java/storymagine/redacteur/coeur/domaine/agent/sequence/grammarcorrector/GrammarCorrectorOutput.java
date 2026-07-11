package storymagine.redacteur.coeur.domaine.agent.sequence.grammarcorrector;

import java.util.List;

/** Output of GrammarCorrector — pairs of (wrong phrase, corrected phrase). */
public record GrammarCorrectorOutput(List<Correction> corrections) {

    /** A single grammar/spelling correction. Caller applies it via {@code String.replace}. */
    public record Correction(String wrongSentence, String correctSentence) {}
}
