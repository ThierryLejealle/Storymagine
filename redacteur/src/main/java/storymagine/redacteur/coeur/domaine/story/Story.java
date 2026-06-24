package storymagine.redacteur.coeur.domaine.story;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** The living story being constructed during writing — distinct from the Scenario which is the spec. */
public class Story {

    private final List<WrittenChapter> chapters         = new ArrayList<>();
    private final WorldState           worldState       = new WorldState();
    private final RepetitionMemory     repetitionMemory = new RepetitionMemory();

    /** Opens a new chapter and registers it as the current one. */
    public WrittenChapter startChapter(ChapterId id) {
        WrittenChapter chapter = new WrittenChapter(id);
        chapters.add(chapter);
        return chapter;
    }

    public Optional<WrittenChapter> currentChapter() {
        return chapters.isEmpty() ? Optional.empty() : Optional.of(chapters.get(chapters.size() - 1));
    }

    public List<WrittenChapter> chapters()         { return List.copyOf(chapters); }
    public WorldState           worldState()       { return worldState; }
    public RepetitionMemory     repetitionMemory() { return repetitionMemory; }

    /** Concatenated summaries of completed chapters, ready for LLM context injection. */
    public String storySoFar() {
        StringBuilder sb = new StringBuilder();
        for (WrittenChapter c : chapters) {
            if (c.summary() != null) {
                sb.append("Chapter ").append(c.id()).append(":\n").append(c.summary()).append("\n\n");
            }
        }
        return sb.toString().trim();
    }
}
