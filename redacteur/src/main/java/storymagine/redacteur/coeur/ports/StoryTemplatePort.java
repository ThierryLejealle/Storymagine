package storymagine.redacteur.coeur.ports;

import java.nio.file.Path;

/** Port for story template operations — copies the initial story structure to a new directory. */
public interface StoryTemplatePort {
    boolean directoryExists(Path path);
    void copyTemplate(Path targetDir);
}
