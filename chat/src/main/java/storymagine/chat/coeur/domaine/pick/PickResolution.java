package storymagine.chat.coeur.domaine.pick;

import java.util.List;

/** What resolving a scenario's texts produced : the texts with their tags replaced, and the draws made. */
public record PickResolution(List<String> texts, PickDraws draws) {

    public PickResolution {
        texts = List.copyOf(texts);
    }
}
