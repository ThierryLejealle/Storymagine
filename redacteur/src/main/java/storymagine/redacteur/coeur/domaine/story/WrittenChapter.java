package storymagine.redacteur.coeur.domaine.story;

import java.util.ArrayList;
import java.util.List;

/** A chapter as it is built during writing: plan and written sequences. */
public class WrittenChapter {

    private final ChapterId             id;
    private String                      plan;
    private List<String>                sequencePlans = List.of();
    private String                      coherence;
    private final List<WrittenSequence> sequences = new ArrayList<>();

    public WrittenChapter(ChapterId id) {
        this.id = id;
    }

    public void setPlan(String plan)                         { this.plan = plan; }
    public void setSequencePlans(List<String> plans)         { this.sequencePlans = List.copyOf(plans); }
    /** Accumulated critic feedback used as input on the next planning attempt. */
    public void setCoherence(String coherence)               { this.coherence = coherence; }
    public void addSequence(WrittenSequence seq)              { sequences.add(seq); }
    /** Clears all written sequences — used before a chapter-level rewrite. */
    public void clearSequences()                             { sequences.clear(); }

    public ChapterId             id()             { return id; }
    /** Plan produced by ChapterPlanner — null until planning phase completes. */
    public String                plan()           { return plan; }
    /** Per-sequence plan slices from ChapterPlanner JSON mode — empty list in free-text mode. */
    public List<String>          sequencePlans()  { return sequencePlans; }
    /** Critic feedback accumulated across planning retries — null on first attempt. */
    public String                coherence()      { return coherence; }
    public List<WrittenSequence> sequences()      { return List.copyOf(sequences); }

    /** Full chapter prose: all sequence texts joined with a blank line. */
    public String fullText() {
        StringBuilder sb = new StringBuilder();
        for (WrittenSequence seq : sequences) {
            if (seq.hasText()) {
                if (sb.length() > 0) sb.append("\n\n");
                sb.append(seq.text());
            }
        }
        return sb.toString();
    }

    // ── Snapshot / restore — for checkpointing across a generation resume ─────

    public record Snapshot(
        ChapterId             id,
        String                plan,
        List<String>          sequencePlans,
        String                coherence,
        List<WrittenSequence> sequences
    ) {}

    public Snapshot snapshot() {
        return new Snapshot(id, plan, sequencePlans, coherence, sequences());
    }

    public static WrittenChapter restore(Snapshot snap) {
        WrittenChapter chapter = new WrittenChapter(snap.id());
        chapter.setPlan(snap.plan());
        chapter.setSequencePlans(snap.sequencePlans());
        chapter.setCoherence(snap.coherence());
        for (WrittenSequence seq : snap.sequences()) {
            chapter.addSequence(seq);
        }
        return chapter;
    }
}
