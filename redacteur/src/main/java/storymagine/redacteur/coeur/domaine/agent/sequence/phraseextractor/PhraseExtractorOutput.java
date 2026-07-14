package storymagine.redacteur.coeur.domaine.agent.sequence.phraseextractor;

/** Output of PhraseExtractor — the relocated phrase as it actually appears in the text, or not found. */
public record PhraseExtractorOutput(String phrase, boolean found) {

    public static PhraseExtractorOutput notFound() {
        return new PhraseExtractorOutput(null, false);
    }
}
