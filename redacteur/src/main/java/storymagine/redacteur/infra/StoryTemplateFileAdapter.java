package storymagine.redacteur.infra;

import storymagine.redacteur.coeur.ports.StoryTemplatePort;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Copies the scenarios/modele directory tree recursively to the new book directory. */
public class StoryTemplateFileAdapter implements StoryTemplatePort {

    private final Path templateDir;

    public StoryTemplateFileAdapter(Path templateDir) {
        this.templateDir = templateDir;
    }

    @Override
    public boolean directoryExists(Path path) {
        return Files.isDirectory(path);
    }

    @Override
    public void copyTemplate(Path targetDir) {
        if (!Files.isDirectory(templateDir)) {
            throw new IllegalStateException(
                "Repertoire modele introuvable : " + templateDir.toAbsolutePath());
        }
        try {
            Files.walk(templateDir).forEach(src -> {
                Path rel = templateDir.relativize(src);
                Path dst = targetDir.resolve(rel);
                try {
                    if (Files.isDirectory(src)) {
                        Files.createDirectories(dst);
                    } else {
                        Files.createDirectories(dst.getParent());
                        Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (IOException | UncheckedIOException e) {
            throw new IllegalStateException("Echec de la copie du modele : " + e.getMessage(), e);
        }
    }
}
