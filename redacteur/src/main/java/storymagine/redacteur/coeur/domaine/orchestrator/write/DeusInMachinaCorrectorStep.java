package storymagine.redacteur.coeur.domaine.orchestrator.write;

import storymagine.redacteur.coeur.domaine.agent.sequence.deusinmachinacorrector.DeusInMachinaCorrector;
import storymagine.redacteur.coeur.domaine.agent.sequence.deusinmachinacorrector.DeusInMachinaCorrectorInput;
import storymagine.redacteur.coeur.domaine.agent.sequence.deusinmachinacorrector.DeusInMachinaCorrectorOutput;
import storymagine.redacteur.coeur.domaine.orchestrator.common.ScenarioFormatters;
import storymagine.redacteur.coeur.domaine.scenario.Chapter;
import storymagine.redacteur.coeur.domaine.scenario.Scenario;

/** Activates DeusInMachinaCorrector to patch instruction leaks in the written prose. */
public class DeusInMachinaCorrectorStep {

    private final DeusInMachinaCorrector agent;

    public DeusInMachinaCorrectorStep(DeusInMachinaCorrector agent) {
        this.agent = agent;
    }

    public DeusInMachinaCorrectorOutput run(String text, Scenario scenario, Chapter chapter) {
        return agent.call(new DeusInMachinaCorrectorInput(
                text,
                ScenarioFormatters.writerConstraints(scenario, chapter)
        ));
    }
}
