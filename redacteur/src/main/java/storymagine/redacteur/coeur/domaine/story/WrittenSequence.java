package storymagine.redacteur.coeur.domaine.story;

/** One written sequence: its optional per-sequence plan slice from ChapterPlanner and its prose (Writer). */
public record WrittenSequence(String sequencePlan, String text) {

    /** True when the Writer has produced prose for this sequence. */
    public boolean hasText() {
        return text != null && !text.isBlank();
    }
}
