package storymagine.redacteur.coeur.domaine.orchestrator.write;

import storymagine.redacteur.coeur.domaine.agent.writer.naturalitycorrector.NaturalityCorrector;
import storymagine.redacteur.coeur.domaine.agent.writer.naturalitycorrector.NaturalityCorrectorInput;
import storymagine.redacteur.coeur.domaine.agent.writer.naturalitycorrector.NaturalityCorrectorOutput;

/** Activates NaturalityCorrector on the full chapter text (all chapter types, SIMPLE and FULL quality). */
public class NaturalityCorrectorStep {

    private final NaturalityCorrector agent;

    public NaturalityCorrectorStep(NaturalityCorrector agent) {
        this.agent = agent;
    }

    public NaturalityCorrectorOutput run(String text) {
        return agent.call(new NaturalityCorrectorInput(text));
    }
}
