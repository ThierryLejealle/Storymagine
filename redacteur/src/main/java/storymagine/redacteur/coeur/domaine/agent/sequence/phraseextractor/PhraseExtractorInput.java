package storymagine.redacteur.coeur.domaine.agent.sequence.phraseextractor;

/** Input for PhraseExtractor — the source text to search, and the citation that failed to match it verbatim. */
public record PhraseExtractorInput(String text, String wrongPhrase) {}
