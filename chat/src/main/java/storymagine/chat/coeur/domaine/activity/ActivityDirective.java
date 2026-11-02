package storymagine.chat.coeur.domaine.activity;

/**
 * One activity command written in an act : {npc-active Elena Marc}, {npc-active *} (every
 * character, as if each were named : explicit), {npc-passive} (no name : every character whose level
 * is not explicit yet) or {npc-reset} (everybody back to present, explicit).
 * namesText is what the author wrote after the keyword, resolved against the Cast later (see
 * ActivityResolver) because a name may have several words.
 * Two more kinds belong to the modes (see NpcMode and ActivityResolver.modeIds) : {npc-mode do on Elena}
 * and {npc-mode do off *} switch a mode on or off for the named characters (or all of them with the
 * star). They never touch an activity level, and mode says which mode (null for the other kinds).
 */
public record ActivityDirective(Kind kind, NpcActivity level, String namesText, NpcMode mode) {

    public enum Kind { LEVEL, RESET, MODE_ON, MODE_OFF }

    public ActivityDirective {
        namesText = namesText == null ? "" : namesText.strip();
    }

    public static ActivityDirective level(NpcActivity level, String namesText) {
        return new ActivityDirective(Kind.LEVEL, level, namesText, null);
    }

    public static ActivityDirective reset() {
        return new ActivityDirective(Kind.RESET, NpcActivity.PRESENT, "", null);
    }

    public static ActivityDirective modeOn(NpcMode mode, String namesText) {
        return new ActivityDirective(Kind.MODE_ON, NpcActivity.PRESENT, namesText, mode);
    }

    public static ActivityDirective modeOff(NpcMode mode, String namesText) {
        return new ActivityDirective(Kind.MODE_OFF, NpcActivity.PRESENT, namesText, mode);
    }

    public boolean isReset() { return kind == Kind.RESET; }

    /** {npc-mode ... on|off ...} : a mode, not an activity level. */
    public boolean isMode() { return kind == Kind.MODE_ON || kind == Kind.MODE_OFF; }

    public boolean isUnnamed() { return kind == Kind.LEVEL && namesText.isEmpty(); }

    /** {npc-active *} : every character named explicitly at once (not the implicit nameless command). */
    public boolean isAll() { return kind == Kind.LEVEL && namesText.equals(ALL); }

    /** {npc-mode do on *} / {npc-mode do off *} : every character at once. */
    public boolean isModeAll() { return isMode() && namesText.equals(ALL); }

    /** The star, never a character's name, so it cannot be mistaken for one. */
    public static final String ALL = "*";
}
