package storymagine.redacteur.coeur.ports;

import storymagine.redacteur.coeur.domaine.orchestrator.GenerationCheckpoint;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/** Port for persisting and reloading generation state, to resume an interrupted run. */
public interface CheckpointPort {

    /** Overwrites the checkpoint of the currently active run with the latest state. */
    void save(GenerationCheckpoint checkpoint);

    /** Deletes the checkpoint of the currently active run — called once generation completes. */
    void clear();

    /** Loads the checkpoint saved in the given run directory, if any. */
    Optional<GenerationCheckpoint> load(Path runDir);

    /** Lists run directories under generatedDir that hold an unfinished checkpoint, most recent first. */
    List<Path> findIncomplete(Path generatedDir);

    CheckpointPort NOOP = new CheckpointPort() {
        @Override public void save(GenerationCheckpoint checkpoint)   {}
        @Override public void clear()                                {}
        @Override public Optional<GenerationCheckpoint> load(Path runDir) { return Optional.empty(); }
        @Override public List<Path> findIncomplete(Path generatedDir)     { return List.of(); }
    };
}
