package storymagine.chat.coeur.domaine.pick;

import java.util.List;

/**
 * What one command handed out : the paragraphs drawn (several for a number or "all"), in the order
 * obtained. scope says which copy of a "{repeat}" block it was drawn in (NONE outside any block) : what
 * the 🎲 table groups by.
 */
public record PickDraw(PickMode mode, PickTag tag, List<String> paragraphs, PickScope scope) {

    public PickDraw {
        paragraphs = List.copyOf(paragraphs);
        scope = scope == null ? PickScope.NONE : scope;
    }

    public PickDraw(PickMode mode, PickTag tag, List<String> paragraphs) {
        this(mode, tag, paragraphs, PickScope.NONE);
    }
}
