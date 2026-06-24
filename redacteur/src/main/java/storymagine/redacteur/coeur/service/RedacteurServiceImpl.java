package storymagine.redacteur.coeur.service;

import storymagine.redacteur.coeur.domaine.orchestrator.GenerationConfig;
import storymagine.redacteur.coeur.domaine.orchestrator.StoryOrchestrator;
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
        var errors = reader.validate(scenarioRoot);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(
                "Scenario invalide :\n" + errors.stream()
                    .map(e -> "  - " + e.message())
                    .collect(Collectors.joining("\n")));
        }
        var scenario = reader.load(scenarioRoot);
        return orchestrator.generate(scenario, config);
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
