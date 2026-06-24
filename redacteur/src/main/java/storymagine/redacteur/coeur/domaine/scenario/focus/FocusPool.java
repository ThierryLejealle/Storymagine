package storymagine.redacteur.coeur.domaine.scenario.focus;

import java.util.List;
import java.util.Optional;

/** The full focus.md content — reservoir of FocusElements referenceable from chapters. */
public class FocusPool {

    private final List<FocusElement> elements;

    public FocusPool(List<FocusElement> elements) {
        this.elements = List.copyOf(elements);
    }

    public List<FocusElement> elements() {
        return elements;
    }

    public Optional<FocusElement> find(String tag) {
        return elements.stream().filter(e -> e.tag().equals(tag)).findFirst();
    }

    public boolean isEmpty() {
        return elements.isEmpty();
    }
}
