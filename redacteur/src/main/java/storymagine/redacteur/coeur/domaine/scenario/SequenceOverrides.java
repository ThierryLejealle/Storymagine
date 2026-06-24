package storymagine.redacteur.coeur.domaine.scenario;

/** Per-sequence values that override global/chapter defaults — last value wins. */
public class SequenceOverrides {

    public static final SequenceOverrides NONE = new SequenceOverrides(null, 0);

    private final String stitch;
    private final int    minWords;

    public SequenceOverrides(String stitch, int minWords) {
        this.stitch   = stitch;
        this.minWords = minWords;
    }

    /** Overrides the global stitch rule — null if not set. */
    public String stitch()   { return stitch; }
    /** Overrides the default sequence word count — 0 if not set. */
    public int    minWords() { return minWords; }
}
