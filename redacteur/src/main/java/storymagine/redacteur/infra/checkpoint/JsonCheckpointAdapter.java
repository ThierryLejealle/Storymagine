package storymagine.redacteur.infra.checkpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import storymagine.redacteur.coeur.domaine.orchestrator.GenerationCheckpoint;
import storymagine.redacteur.coeur.ports.CheckpointPort;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

/** Persists generation checkpoints as checkpoint.json inside the run directory, via Jackson. */
public class JsonCheckpointAdapter implements CheckpointPort {

    private static final String FILE_NAME = "checkpoint.json";

    private final ObjectMapper   mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private final Supplier<Path> runDir;

    /** runDir supplier is queried lazily — may return null before the first log write. */
    public JsonCheckpointAdapter(Supplier<Path> runDir) {
        this.runDir = runDir;
    }

    @Override
    public void save(GenerationCheckpoint checkpoint) {
        Path dir = runDir.get();
        if (dir == null) return;
        try {
            Files.createDirectories(dir);
            mapper.writeValue(dir.resolve(FILE_NAME).toFile(), checkpoint);
        } catch (IOException ignored) {}
    }

    @Override
    public void clear() {
        Path dir = runDir.get();
        if (dir == null) return;
        try {
            Files.deleteIfExists(dir.resolve(FILE_NAME));
        } catch (IOException ignored) {}
    }

    @Override
    public Optional<GenerationCheckpoint> load(Path targetRunDir) {
        Path file = targetRunDir.resolve(FILE_NAME);
        if (!Files.exists(file)) return Optional.empty();
        try {
            return Optional.of(mapper.readValue(file.toFile(), GenerationCheckpoint.class));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<Path> findIncomplete(Path generatedDir) {
        if (!Files.isDirectory(generatedDir)) return List.of();
        List<Path> result = new ArrayList<>();
        try (Stream<Path> dirs = Files.list(generatedDir)) {
            dirs.filter(Files::isDirectory)
                .filter(dir -> Files.exists(dir.resolve(FILE_NAME)))
                .forEach(result::add);
        } catch (IOException ignored) {
            return List.of();
        }
        result.sort(Comparator.comparing(Path::getFileName).reversed());
        return result;
    }
}
