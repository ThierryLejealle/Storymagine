package storymagine.chat.coeur.domaine.pick;

import java.util.List;

/** What unfolding the "{repeat}" blocks of a scenario's texts produced : the texts, and one draw per block (the paragraphs in play order). */
public record RepeatExpansion(List<String> texts, List<PickDraw> draws) {

    public RepeatExpansion {
        texts = List.copyOf(texts);
        draws = List.copyOf(draws);
    }
}
