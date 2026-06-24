package storymagine.redacteur.coeur.service;

/** Creates the directory structure for a new story from the built-in template. */
public interface InitStoryService {
    InitStoryResult init(InitStoryCommand command);
}
