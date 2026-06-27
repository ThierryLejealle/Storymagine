package storymagine.redacteur.coeur.domaine.orchestrator.write;

import storymagine.redacteur.coeur.domaine.agent.sequence.proofreadercorrector.ProofreaderCorrector;
import storymagine.redacteur.coeur.domaine.agent.sequence.proofreadercorrector.ProofreaderCorrectorInput;
import storymagine.redacteur.coeur.domaine.agent.sequence.proofreadercorrector.ProofreaderCorrectorOutput;

/** Activates ProofreaderCorrector on a sequence text. */
public class ProofreaderCorrectorStep {

    private final ProofreaderCorrector agent;

    public ProofreaderCorrectorStep(ProofreaderCorrector agent) {
        this.agent = agent;
    }

    public ProofreaderCorrectorOutput run(String text) {
        return agent.call(new ProofreaderCorrectorInput(text));
    }
}
