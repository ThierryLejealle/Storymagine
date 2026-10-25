package storymagine.chat.infra;

import storymagine.chat.coeur.domaine.pick.PickLibrary;
import storymagine.chat.coeur.domaine.pick.PickParagraphs;
import storymagine.chat.coeur.domaine.pick.PickTag;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** PickLibrary reading {scenarioDir}/picks/{tag}.txt — see PickParagraphs for the file format. */
final class PickFileLibrary implements PickLibrary {

    static final String PICKS_DIR = "picks";

    private final Path picksDir;

    PickFileLibrary(Path scenarioDir) {
        this.picksDir = scenarioDir.resolve(PICKS_DIR);
    }

    @Override
    public PickParagraphs paragraphs(PickTag tag) {
        Path file = picksDir.resolve(tag.name() + ".txt");
        if (!Files.isRegularFile(file)) {
            throw new IllegalStateException("[PICK: " + tag + "] : fichier introuvable " + file);
        }
        PickParagraphs paragraphs;
        try {
            paragraphs = PickParagraphs.parse(Files.readString(file, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("Lecture impossible : " + file, e);
        }
        if (paragraphs.size() == 0) {
            throw new IllegalStateException("[PICK: " + tag + "] : aucun paragraphe dans " + file);
        }
        return paragraphs;
    }
}
