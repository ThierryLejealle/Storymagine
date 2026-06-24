package storymagine.redacteur.coeur.service;

import java.nio.file.Path;

/** Result of a story initialisation — the directory that was created. */
public record InitStoryResult(Path bookDir) {}
