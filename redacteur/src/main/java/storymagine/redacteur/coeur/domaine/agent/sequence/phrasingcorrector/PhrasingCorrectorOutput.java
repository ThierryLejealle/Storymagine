package storymagine.redacteur.coeur.domaine.agent.sequence.phrasingcorrector;

import java.util.List;

/** Output of PhrasingCorrector — pairs of (wrong phrase, corrected phrase). */
public record PhrasingCorrectorOutput(List<Correction> corrections) {

    /** A single phrasing correction. Caller applies it via {@code String.replace}. */
    public record Correction(String wrongSentence, String correctSentence) {}
}
