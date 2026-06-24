package storymagine.redacteur.infra;

import storymagine.redacteur.coeur.domaine.story.Story;
import storymagine.redacteur.coeur.domaine.story.WrittenChapter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Writes a generated Story to disk: one file per chapter + full assembled story. */
public class StoryExporter {

    public Path export(Story story, Path outputDir) throws IOException {
        Files.createDirectories(outputDir);

        StringBuilder full = new StringBuilder();
        int chapNum = 1;
        for (WrittenChapter wc : story.chapters()) {
            String chapterText = buildChapterText(wc, chapNum);
            String filename    = String.format("chapitre-%02d.txt", chapNum);
            Files.writeString(outputDir.resolve(filename), chapterText, StandardCharsets.UTF_8);
            if (full.length() > 0) full.append("\n\n");
            full.append(chapterText);
            chapNum++;
        }

        Path fullFile = outputDir.resolve("histoire-complete.txt");
        Files.writeString(fullFile, full.toString().strip(), StandardCharsets.UTF_8);
        return fullFile;
    }

    private String buildChapterText(WrittenChapter wc, int num) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Chapitre ").append(num).append(" : ").append(wc.id()).append(" ===\n\n");
        if (wc.plan() != null && !wc.plan().isBlank()) {
            sb.append("--- PLAN ---\n").append(wc.plan()).append("\n\n--- TEXTE ---\n\n");
        }
        String text = wc.fullText();
        if (text != null && !text.isBlank()) sb.append(text);
        return sb.toString();
    }
}
