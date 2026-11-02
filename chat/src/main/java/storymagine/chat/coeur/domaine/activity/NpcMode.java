package storymagine.chat.coeur.domaine.activity;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A mode an author switches on or off for characters with "{npc-mode name on|off names}" (see
 * ActivityDirective). Independent of the activity level : a character is active, present, passive
 * or absent, and may also be in one or several modes. DO : the character holds the narrator's pen,
 * as if the player prefixed every message with "DO:" (see ChatServiceImpl) ; its "{npc-starts}"
 * opening then becomes a DO too.
 */
public enum NpcMode {
    DO("do");

    private final String keyword;

    NpcMode(String keyword) {
        this.keyword = keyword;
    }

    public String keyword() {
        return keyword;
    }

    /** The mode written by the author, any case. */
    public static Optional<NpcMode> fromKeyword(String word) {
        String w = word.strip().toLowerCase(Locale.ROOT);
        return Arrays.stream(values()).filter(mode -> mode.keyword.equals(w)).findFirst();
    }

    /** "do" (or "do, other" once there are several) : the modes an error message can offer. */
    public static String keywords() {
        return Arrays.stream(values()).map(NpcMode::keyword).collect(Collectors.joining(", "));
    }
}
