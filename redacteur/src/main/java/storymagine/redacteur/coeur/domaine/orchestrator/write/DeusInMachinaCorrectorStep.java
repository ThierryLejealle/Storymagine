package storymagine.redacteur.coeur.domaine.orchestrator.write;

import storymagine.redacteur.coeur.domaine.agent.commun.RetryStrategy;
import storymagine.redacteur.coeur.domaine.agent.sequence.deusinmachinacorrector.DeusInMachinaCorrector;
import storymagine.redacteur.coeur.domaine.agent.sequence.deusinmachinacorrector.DeusInMachinaCorrectorInput;
import storymagine.redacteur.coeur.domaine.agent.sequence.deusinmachinacorrector.DeusInMachinaCorrectorOutput;
import storymagine.redacteur.coeur.domaine.orchestrator.common.ScenarioFormatters;
import storymagine.redacteur.coeur.domaine.scenario.Chapter;
import storymagine.redacteur.coeur.domaine.scenario.Scenario;
import storymagine.redacteur.coeur.domaine.scenario.Sequence;

/** Activates DeusInMachinaCorrector to patch instruction leaks in the written prose. */
public class DeusInMachinaCorrectorStep {

    private final DeusInMachinaCorrector agent;

    public DeusInMachinaCorrectorStep(DeusInMachinaCorrector agent) {
        this.agent = agent;
    }

    public DeusInMachinaCorrectorOutput run(String text, Scenario scenario, Chapter chapter,
                                             Sequence sequence, String sequencePlan) {
        return agent.call(new DeusInMachinaCorrectorInput(
                text,
                String.join("\n", ScenarioFormatters.writerChecks(scenario, chapter, sequence)),
                sequence.directive(),
                sequencePlan
        ));
    }

    public RetryStrategy retryStrategy() {
        return agent.retryStrategy();
    }
}
