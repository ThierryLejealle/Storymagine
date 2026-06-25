package storymagine.redacteur.infra;

import storymagine.redacteur.coeur.domaine.story.Story;
import storymagine.redacteur.coeur.ports.HtmlExportPort;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

/** Writes story.html into the run directory after each chapter completes. */
public class HtmlFileExportAdapter implements HtmlExportPort {

    private final Supplier<Path> runDir;

    /** runDir supplier is queried lazily — may return null before the first log write. */
    public HtmlFileExportAdapter(Supplier<Path> runDir) {
        this.runDir = runDir;
    }

    @Override
    public void exportHtml(String bookTitle, Story story) {
        Path dir = runDir.get();
        if (dir == null) return;
        String html = HtmlExporter.generate(bookTitle, story.chapters());
        try {
            Files.writeString(dir.resolve("story.html"), html, StandardCharsets.UTF_8);
        } catch (IOException ignored) {}
    }
}
