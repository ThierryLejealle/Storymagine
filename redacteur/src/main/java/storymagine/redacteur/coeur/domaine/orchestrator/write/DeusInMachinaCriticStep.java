package storymagine.redacteur.coeur.domaine.orchestrator.write;

import storymagine.redacteur.coeur.domaine.agent.sequence.deusinmachinacritic.DeusInMachinaCritic;
import storymagine.redacteur.coeur.domaine.agent.sequence.deusinmachinacritic.DeusInMachinaCriticInput;
import storymagine.redacteur.coeur.domaine.agent.sequence.deusinmachinacritic.DeusInMachinaCriticOutput;
import storymagine.redacteur.coeur.domaine.orchestrator.common.ScenarioFormatters;
import storymagine.redacteur.coeur.domaine.scenario.Chapter;
import storymagine.redacteur.coeur.domaine.scenario.Scenario;

/** Activates DeusInMachinaCritic to detect instruction leaks in the written prose. */
public class DeusInMachinaCriticStep {

    private final DeusInMachinaCritic agent;

    public DeusInMachinaCriticStep(DeusInMachinaCritic agent) {
        this.agent = agent;
    }

    public DeusInMachinaCriticOutput run(String text, Scenario scenario, Chapter chapter) {
        return agent.call(new DeusInMachinaCriticInput(
                text,
                ScenarioFormatters.writerConstraints(scenario, chapter)
        ));
    }
}
