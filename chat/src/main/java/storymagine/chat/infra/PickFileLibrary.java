package storymagine.chat.infra;

import storymagine.chat.coeur.domaine.pick.PickLibrary;
import storymagine.chat.coeur.domaine.pick.PickParagraphs;
import storymagine.chat.coeur.domaine.pick.PickTag;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * PickLibrary reading {scenarioDir}/picks/{tag}.txt, or for a shared tag "/lib/{path}" the file
 * lib/{path}.txt of the directory that holds the scenarios (the one the player chose, next to the scenario's
 * own directory) — see PickParagraphs for the file format.
 */
final class PickFileLibrary implements PickLibrary {

    static final String PICKS_DIR = "picks";
    static final String SHARED_DIR = "lib";

    private final Path picksDir;
    private final Path sharedDir;

    PickFileLibrary(Path scenarioDir) {
        this.picksDir = scenarioDir.resolve(PICKS_DIR);
        Path scenariosRoot = scenarioDir.getParent();
        this.sharedDir = (scenariosRoot == null ? Path.of("") : scenariosRoot).resolve(SHARED_DIR);
    }

    @Override
    public PickParagraphs paragraphs(PickTag tag) {
        Path file = tag.isShared() ? sharedDir.resolve(tag.sharedPath() + ".txt") : picksDir.resolve(tag.name() + ".txt");
        if (!Files.isRegularFile(file)) {
            throw new IllegalStateException(tag.file() + " : fichier introuvable " + file
                + (tag.isShared() ? " (une bibliotheque /lib/ se trouve dans le dossier lib/ a cote des scenarios)" : ""));
        }
        PickParagraphs paragraphs;
        try {
            paragraphs = PickParagraphs.parse(Files.readString(file, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("Lecture impossible : " + file, e);
        }
        if (paragraphs.size() == 0) {
            throw new IllegalStateException(tag.file() + " : aucun paragraphe dans " + file);
        }
        return paragraphs;
    }
}
