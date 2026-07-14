package storymagine.redacteur.coeur.domaine.orchestrator.write;

import storymagine.redacteur.coeur.domaine.agent.sequence.phraseextractor.PhraseExtractor;
import storymagine.redacteur.coeur.domaine.agent.sequence.phraseextractor.PhraseExtractorInput;
import storymagine.redacteur.coeur.domaine.agent.sequence.phraseextractor.PhraseExtractorOutput;

/** Activates PhraseExtractor to relocate a Corrector citation that failed to match the text verbatim. */
public class PhraseExtractorStep {

    private final PhraseExtractor agent;

    public PhraseExtractorStep(PhraseExtractor agent) {
        this.agent = agent;
    }

    public PhraseExtractorOutput run(String text, String wrongPhrase) {
        return agent.call(new PhraseExtractorInput(text, wrongPhrase));
    }
}
