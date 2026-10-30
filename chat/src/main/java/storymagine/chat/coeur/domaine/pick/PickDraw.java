package storymagine.chat.coeur.domaine.pick;

import java.util.List;

/** What one "{pick mode name}" command handed out : the paragraphs drawn (several for a number or "all"), in the order obtained. */
public record PickDraw(PickMode mode, PickTag tag, List<String> paragraphs) {

    public PickDraw {
        paragraphs = List.copyOf(paragraphs);
    }
}
