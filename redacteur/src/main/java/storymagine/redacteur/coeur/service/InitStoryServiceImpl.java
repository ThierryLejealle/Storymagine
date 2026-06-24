package storymagine.redacteur.coeur.service;

import storymagine.redacteur.coeur.ports.StoryTemplatePort;

import java.nio.file.Path;

public class InitStoryServiceImpl implements InitStoryService {

    private final StoryTemplatePort template;

    public InitStoryServiceImpl(StoryTemplatePort template) {
        this.template = template;
    }

    @Override
    public InitStoryResult init(InitStoryCommand command) {
        if (command.bookName() == null || command.bookName().isBlank()) {
            throw new IllegalArgumentException("Le nom de l'histoire ne peut pas etre vide.");
        }
        Path bookDir = command.storyRoot().resolve(command.bookName());
        if (template.directoryExists(bookDir) && !command.overwrite()) {
            throw new IllegalStateException(
                "Le repertoire '" + bookDir + "' existe deja. Confirmez l'ecrasement.");
        }
        template.copyTemplate(bookDir);
        return new InitStoryResult(bookDir);
    }
}
