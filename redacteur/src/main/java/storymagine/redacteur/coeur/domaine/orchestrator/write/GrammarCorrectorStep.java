package storymagine.redacteur.coeur.domaine.orchestrator.write;

import storymagine.redacteur.coeur.domaine.agent.commun.RetryStrategy;
import storymagine.redacteur.coeur.domaine.agent.sequence.grammarcorrector.GrammarCorrector;
import storymagine.redacteur.coeur.domaine.agent.sequence.grammarcorrector.GrammarCorrectorInput;
import storymagine.redacteur.coeur.domaine.agent.sequence.grammarcorrector.GrammarCorrectorOutput;

/** Activates GrammarCorrector on a sequence text. */
public class GrammarCorrectorStep {

    private final GrammarCorrector agent;

    public GrammarCorrectorStep(GrammarCorrector agent) {
        this.agent = agent;
    }

    public GrammarCorrectorOutput run(String text) {
        return agent.call(new GrammarCorrectorInput(text));
    }

    public RetryStrategy retryStrategy() {
        return agent.retryStrategy();
    }
}
