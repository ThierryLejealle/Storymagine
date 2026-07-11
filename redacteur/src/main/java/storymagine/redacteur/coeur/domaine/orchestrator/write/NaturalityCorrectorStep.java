package storymagine.redacteur.coeur.domaine.orchestrator.write;

import storymagine.redacteur.coeur.domaine.agent.commun.RetryStrategy;
import storymagine.redacteur.coeur.domaine.agent.sequence.naturalitycorrector.NaturalityCorrector;
import storymagine.redacteur.coeur.domaine.agent.sequence.naturalitycorrector.NaturalityCorrectorInput;
import storymagine.redacteur.coeur.domaine.agent.sequence.naturalitycorrector.NaturalityCorrectorOutput;

/** Activates NaturalityCorrector on the full chapter text (all chapter types, SIMPLE and FULL quality). */
public class NaturalityCorrectorStep {

    private final NaturalityCorrector agent;

    public NaturalityCorrectorStep(NaturalityCorrector agent) {
        this.agent = agent;
    }

    public NaturalityCorrectorOutput run(String text) {
        return agent.call(new NaturalityCorrectorInput(text));
    }

    public RetryStrategy retryStrategy() {
        return agent.retryStrategy();
    }
}
