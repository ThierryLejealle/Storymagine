package storymagine.redacteur.coeur.domaine.agent.writer.proofreader;

import java.util.List;

/** Output of Proofreader — pairs of (wrong phrase, corrected phrase). */
public record ProofreaderOutput(List<Correction> corrections) {

    /** A single proofreading correction. Caller applies it via {@code String.replace}. */
    public record Correction(String wrongSentence, String correctSentence) {}
}
