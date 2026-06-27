package storymagine.redacteur.coeur.domaine.agent.sequence.proofreadercorrector;

import java.util.List;

/** Output of ProofreaderCorrector — pairs of (wrong phrase, corrected phrase). */
public record ProofreaderCorrectorOutput(List<Correction> corrections) {

    /** A single proofreading correction. Caller applies it via {@code String.replace}. */
    public record Correction(String wrongSentence, String correctSentence) {}
}
