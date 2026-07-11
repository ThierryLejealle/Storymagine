package storymagine.redacteur.coeur.domaine.orchestrator.write;

import storymagine.redacteur.coeur.domaine.agent.commun.RetryStrategy;
import storymagine.redacteur.coeur.domaine.agent.sequence.phrasingcorrector.PhrasingCorrector;
import storymagine.redacteur.coeur.domaine.agent.sequence.phrasingcorrector.PhrasingCorrectorInput;
import storymagine.redacteur.coeur.domaine.agent.sequence.phrasingcorrector.PhrasingCorrectorOutput;

/** Activates PhrasingCorrector on a sequence text. */
public class PhrasingCorrectorStep {

    private final PhrasingCorrector agent;

    public PhrasingCorrectorStep(PhrasingCorrector agent) {
        this.agent = agent;
    }

    public PhrasingCorrectorOutput run(String text) {
        return agent.call(new PhrasingCorrectorInput(text));
    }

    public RetryStrategy retryStrategy() {
        return agent.retryStrategy();
    }
}
