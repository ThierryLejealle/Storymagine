package storymagine.chat.coeur.domaine.activity;

import java.util.Locale;
import java.util.Optional;

/**
 * How a character takes part in the scene : ACTIVE speaks at every exchange, PRESENT answers when
 * mentioned (and keeps the thread of a conversation), PASSIVE only reacts briefly now and then,
 * ABSENT never speaks and is not in the scene. The declaration order is the order the player's click
 * cycles through (see ChatSession.cycleActivity).
 */
public enum NpcActivity {
    ACTIVE, PRESENT, PASSIVE, ABSENT;

    /** The word written after "npc-" in a scenario command : {npc-active ...}. */
    public String keyword() { return name().toLowerCase(Locale.ROOT); }

    public NpcActivity next() { return values()[(ordinal() + 1) % values().length]; }

    public boolean isInScene() { return this != ABSENT; }

    public static Optional<NpcActivity> fromKeyword(String keyword) {
        for (NpcActivity level : values()) {
            if (level.keyword().equalsIgnoreCase(keyword)) return Optional.of(level);
        }
        return Optional.empty();
    }
}
