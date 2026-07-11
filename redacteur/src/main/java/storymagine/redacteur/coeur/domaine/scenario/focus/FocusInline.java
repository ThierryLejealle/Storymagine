package storymagine.redacteur.coeur.domaine.scenario.focus;

import storymagine.redacteur.coeur.domaine.scenario.Requirement;

/**
 * Inline focus text written directly in the chapter — not from the pool.
 * check is optional, using the same "texte | check" syntax as Requirement
 * (a single value with no "|" is used as text only, no check).
 */
public record FocusInline(String text, String check) implements FocusItem {

    public static FocusInline parse(String raw) {
        if (raw == null) return new FocusInline("", null);
        String[] parts = Requirement.splitOnPipe(raw);
        return new FocusInline(parts[0], parts[1]);
    }
}
