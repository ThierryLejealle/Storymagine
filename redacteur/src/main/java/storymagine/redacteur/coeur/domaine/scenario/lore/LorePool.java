package storymagine.redacteur.coeur.domaine.scenario.lore;

import java.util.List;
import java.util.Optional;

/** The full lore.md content — reservoir of LoreElements referenceable from chapters. */
public class LorePool {

    public static final LorePool EMPTY = new LorePool(List.of());

    private final List<LoreElement> elements;

    public LorePool(List<LoreElement> elements) {
        this.elements = List.copyOf(elements);
    }

    public List<LoreElement> elements() {
        return elements;
    }

    public Optional<LoreElement> find(String tag) {
        return elements.stream().filter(e -> e.tag().equalsIgnoreCase(tag)).findFirst();
    }

    public boolean isEmpty() {
        return elements.isEmpty();
    }
}
