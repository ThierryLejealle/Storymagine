package storymagine.redacteur.coeur.service;

import java.nio.file.Path;

/** Command to initialise a new story: where to create it, what to name it, and whether to overwrite. */
public record InitStoryCommand(Path storyRoot, String bookName, boolean overwrite) {}
