package storymagine.redacteur.coeur.service;

import storymagine.redacteur.coeur.domaine.orchestrator.GenerationConfig;
import storymagine.redacteur.coeur.domaine.story.Story;
import storymagine.redacteur.coeur.ports.ScenarioError;

import java.nio.file.Path;
import java.util.List;

/** Public API of the redacteur module exposed to the CLI. */
public interface RedacteurService {

    List<ScenarioError> validate(Path scenarioRoot);

    Story generate(Path scenarioRoot, GenerationConfig config);

    List<Path> listScenarios(Path root);
}
