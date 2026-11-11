package storymagine.chat.coeur.domaine.activity;

import java.util.Locale;
import java.util.Optional;

/**
 * How a character takes part in the scene : ACTIVE speaks at every exchange, PRESENT answers when
 * mentioned (and keeps the thread of a conversation), PASSIVE only reacts briefly now and then,
 * ABSENT never speaks and is not in the scene. The declaration order is the order the player's click
 * cycles through (see ChatSession.cycleActivity). OUTSIDE is not a place in that cycle : the character is
 * not in the story at all (not arrived yet, or gone — see {npc-arrives} / {npc-leaves}) : no vignette, no
 * sheet in the others' prompts, and a click never reaches it. Unlike ABSENT, which stays visible.
 */
public enum NpcActivity {
    ACTIVE, PRESENT, PASSIVE, ABSENT, OUTSIDE;

    /** The word written after "npc-" in a scenario command : {npc-active ...}. */
    public String keyword() { return name().toLowerCase(Locale.ROOT); }

    /** The next level of the player's click cycle (OUTSIDE is never part of it). */
    public NpcActivity next() { return this == ABSENT || this == OUTSIDE ? ACTIVE : values()[ordinal() + 1]; }

    public boolean isInScene() { return this != ABSENT && this != OUTSIDE; }

    public boolean isOutside() { return this == OUTSIDE; }

    public static Optional<NpcActivity> fromKeyword(String keyword) {
        for (NpcActivity level : values()) {
            if (level.keyword().equalsIgnoreCase(keyword)) return Optional.of(level);
        }
        return Optional.empty();
    }
}
