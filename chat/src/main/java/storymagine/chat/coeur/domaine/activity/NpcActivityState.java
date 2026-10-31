package storymagine.chat.coeur.domaine.activity;

/** A character's activity level at one point of the story, with where that level comes from. */
public record NpcActivityState(NpcActivity level, ActivitySource source) {

    /** What every character is before any command : present, implicit. */
    public static final NpcActivityState DEFAULT = new NpcActivityState(NpcActivity.PRESENT, ActivitySource.IMPLICIT);
}
