package storymagine.chat.coeur.domaine.activity;

/**
 * One activity command written in an act : {npc-active Elena Marc}, {npc-active *} (every
 * character, as if each were named : explicit), {npc-passive} (no name : every character whose level
 * is not explicit yet) or {npc-reset} (everybody back to present, explicit).
 * namesText is what the author wrote after the keyword, resolved against the Cast later (see
 * ActivityResolver) because a name may have several words.
 */
public record ActivityDirective(Kind kind, NpcActivity level, String namesText) {

    public enum Kind { LEVEL, RESET }

    public ActivityDirective {
        namesText = namesText == null ? "" : namesText.strip();
    }

    public static ActivityDirective level(NpcActivity level, String namesText) {
        return new ActivityDirective(Kind.LEVEL, level, namesText);
    }

    public static ActivityDirective reset() {
        return new ActivityDirective(Kind.RESET, NpcActivity.PRESENT, "");
    }

    public boolean isReset() { return kind == Kind.RESET; }

    public boolean isUnnamed() { return kind == Kind.LEVEL && namesText.isEmpty(); }

    /** {npc-active *} : every character named explicitly at once (not the implicit nameless command). */
    public boolean isAll() { return kind == Kind.LEVEL && namesText.equals(ALL); }

    /** The star, never a character's name, so it cannot be mistaken for one. */
    public static final String ALL = "*";
}
