package storymagine.redacteur.coeur.domaine.orchestrator.write;

import storymagine.redacteur.coeur.domaine.agent.writer.deusinmachinachecker.DeusInMachinaChecker;
import storymagine.redacteur.coeur.domaine.agent.writer.deusinmachinachecker.DeusInMachinaCheckerInput;
import storymagine.redacteur.coeur.domaine.agent.writer.deusinmachinachecker.DeusInMachinaCheckerOutput;
import storymagine.redacteur.coeur.domaine.orchestrator.common.ScenarioFormatters;
import storymagine.redacteur.coeur.domaine.scenario.Chapter;
import storymagine.redacteur.coeur.domaine.scenario.Scenario;

/** Activates DeusInMachinaChecker to detect instruction leaks in the written prose. */
public class DeusInMachinaCheckerStep {

    private final DeusInMachinaChecker agent;

    public DeusInMachinaCheckerStep(DeusInMachinaChecker agent) {
        this.agent = agent;
    }

    public DeusInMachinaCheckerOutput run(String text, Scenario scenario, Chapter chapter) {
        return agent.call(new DeusInMachinaCheckerInput(
                text,
                ScenarioFormatters.writerConstraints(scenario, chapter)
        ));
    }
}
