package storymagine.redacteur.coeur.service;

import storymagine.redacteur.coeur.domaine.orchestrator.GenerationConfig;
import storymagine.redacteur.coeur.domaine.orchestrator.StoryOrchestrator;
import storymagine.redacteur.coeur.domaine.scenario.Scenario;
import storymagine.redacteur.coeur.domaine.story.Story;
import storymagine.redacteur.coeur.ports.ScenarioError;
import storymagine.redacteur.coeur.ports.ScenarioReaderPort;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class RedacteurServiceImpl implements RedacteurService {

    private final ScenarioReaderPort reader;
    private final StoryOrchestrator  orchestrator;

    public RedacteurServiceImpl(ScenarioReaderPort reader, StoryOrchestrator orchestrator) {
        this.reader      = reader;
        this.orchestrator = orchestrator;
    }

    @Override
    public List<ScenarioError> validate(Path scenarioRoot) {
        return reader.validate(scenarioRoot);
    }

    @Override
    public Story generate(Path scenarioRoot, GenerationConfig config) {
        return orchestrator.generate(loadValidScenario(scenarioRoot), config);
    }

    @Override
    public Story resume(Path scenarioRoot, Path runDir) {
        return orchestrator.resume(loadValidScenario(scenarioRoot), runDir);
    }

    private Scenario loadValidScenario(Path scenarioRoot) {
        var errors = reader.validate(scenarioRoot);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(
                "Scenario invalide :\n" + errors.stream()
                    .map(e -> "  - " + e.message())
                    .collect(Collectors.joining("\n")));
        }
        return reader.load(scenarioRoot);
    }

    @Override
    public List<Path> listScenarios(Path root) {
        if (!root.toFile().isDirectory()) return Collections.emptyList();
        try {
            return Files.list(root)
                .filter(p -> p.toFile().isDirectory())
                .filter(p -> p.resolve("scenario.md").toFile().exists()
                          || p.resolve("scenario.yaml").toFile().exists())
                .sorted()
                .collect(Collectors.toList());
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }
}
