package storymagine.redacteur.coeur.ports;

import storymagine.redacteur.coeur.domaine.scenario.Scenario;

import java.nio.file.Path;
import java.util.List;

public interface ScenarioReaderPort {

    /** Validates a scenario directory and returns all errors found. Empty list means valid. */
    List<ScenarioError> validate(Path scenarioRoot);

    /** Loads and fully resolves a scenario directory into a Scenario aggregate. */
    Scenario load(Path scenarioRoot);
}
