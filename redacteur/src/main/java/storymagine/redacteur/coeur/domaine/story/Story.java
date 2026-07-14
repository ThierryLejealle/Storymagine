package storymagine.redacteur.coeur.domaine.story;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** The living story being constructed during writing — distinct from the Scenario which is the spec. */
public class Story {

    private final List<WrittenChapter> chapters         = new ArrayList<>();
    private final WorldState           worldState       = new WorldState();
    private final RepetitionMemory     repetitionMemory = new RepetitionMemory();
    private int                        currentIndex     = -1;
    private String                     summary          = "";
    private int                        chapterSummaryDivisor = 5;

    /** Opens a new chapter and registers it as the current one. */
    public WrittenChapter startChapter(ChapterId id) {
        WrittenChapter chapter = new WrittenChapter(id);
        chapters.add(chapter);
        currentIndex = chapters.size() - 1;
        return chapter;
    }

    /** Makes an already-planned chapter the current one, for the writing phase. */
    public WrittenChapter activateChapter(ChapterId id) {
        for (int i = 0; i < chapters.size(); i++) {
            if (chapters.get(i).id().equals(id)) {
                currentIndex = i;
                return chapters.get(i);
            }
        }
        throw new IllegalArgumentException("Aucun chapitre planifie pour : " + id);
    }

    public Optional<WrittenChapter> currentChapter() {
        return currentIndex < 0 ? Optional.empty() : Optional.of(chapters.get(currentIndex));
    }

    public List<WrittenChapter> chapters()         { return List.copyOf(chapters); }
    public WorldState           worldState()       { return worldState; }
    public RepetitionMemory     repetitionMemory() { return repetitionMemory; }

    /** Running story-so-far summary, ready for LLM context injection. */
    public String summary() { return summary; }

    /** Current per-chapter word budget divisor for ChapterSummarizer — grows via compressSummary(). */
    public int chapterSummaryDivisor() { return chapterSummaryDivisor; }

    /** Appends a newly written chapter's independent summary to the running story summary. */
    public void appendChapterSummary(String chapterSummary) {
        summary = summary.isBlank() ? chapterSummary : summary + "\n\n" + chapterSummary;
    }

    /** Replaces the running summary with a condensed version and doubles the per-chapter divisor. */
    public void compressSummary(String compressedSummary) {
        summary = compressedSummary;
        chapterSummaryDivisor *= 2;
    }

    // ── Snapshot / restore — for checkpointing across a generation resume ─────

    public record Snapshot(
        List<WrittenChapter.Snapshot> chapters,
        WorldState.Snapshot           worldState,
        RepetitionMemory.Snapshot     repetitionMemory,
        String                        summary,
        int                           chapterSummaryDivisor
    ) {}

    public Snapshot snapshot() {
        return new Snapshot(
            chapters.stream().map(WrittenChapter::snapshot).toList(),
            worldState.snapshot(),
            repetitionMemory.snapshot(),
            summary,
            chapterSummaryDivisor
        );
    }

    public static Story restore(Snapshot snap) {
        Story story = new Story();
        for (WrittenChapter.Snapshot chapterSnap : snap.chapters()) {
            story.chapters.add(WrittenChapter.restore(chapterSnap));
        }
        story.worldState.restore(snap.worldState());
        story.repetitionMemory.restore(snap.repetitionMemory());
        story.summary = snap.summary();
        story.chapterSummaryDivisor = snap.chapterSummaryDivisor();
        story.currentIndex = story.chapters.isEmpty() ? -1 : story.chapters.size() - 1;
        return story;
    }
}
