package storymagine.chat.coeur.domaine.activity;

/**
 * A level the player forced by clicking a character. anchor is the number of the heading it hangs
 * from (the parent of the act where the click happened, "" for the root) : it holds for every act
 * under that heading and disappears as soon as the story leaves it (see ChatSession).
 */
public record ForcedActivity(NpcActivity level, String anchor) {

    public ForcedActivity {
        anchor = anchor == null ? "" : anchor;
    }
}
