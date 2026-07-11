package storymagine.redacteur.coeur.domaine.scenario;

/**
 * A single narrative rule, expressed for its two possible audiences: the constraint given
 * to generator agents (Writer/Planner) and the check given to verifier agents (Critics).
 *
 * Textual convention parsed by {@link #parse(String)}:
 *   "texte"              -> same text used as constraint and as check
 *   "contrainte | check" -> distinct formulations for each audience
 *   "contrainte |"       -> constraint only, no check (ignored by Critics)
 *   "| check"            -> check only, no constraint (ignored by Writer/Planner)
 */
public record Requirement(String constraint, String check) {

    public static Requirement parse(String raw) {
        if (raw == null) return new Requirement("", "");
        String[] parts = splitOnPipe(raw);
        return parts[1] == null ? new Requirement(parts[0], parts[0]) : new Requirement(parts[0], parts[1]);
    }

    /**
     * Splits "before | after" on the first '|', trimming both sides.
     * Returns {before, null} when there is no '|' at all — callers decide what "no pipe"
     * means for their own defaulting rule (Requirement defaults to "same value for both";
     * other callers, e.g. FocusInline, may default the second side to absent instead).
     */
    public static String[] splitOnPipe(String raw) {
        String text = raw.strip();
        int sep = text.indexOf('|');
        if (sep < 0) return new String[] { text, null };
        return new String[] { text.substring(0, sep).strip(), text.substring(sep + 1).strip() };
    }
}
