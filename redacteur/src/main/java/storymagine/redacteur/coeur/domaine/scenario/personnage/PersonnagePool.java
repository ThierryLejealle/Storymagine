package storymagine.redacteur.coeur.domaine.scenario.personnage;

import java.util.List;
import java.util.Optional;

/** All character sheets loaded from the characters/ directory. */
public class PersonnagePool {

    private final List<Personnage> personnages;

    public PersonnagePool(List<Personnage> personnages) {
        this.personnages = List.copyOf(personnages);
    }

    public List<Personnage> all() {
        return personnages;
    }

    public Optional<Personnage> find(String id) {
        return personnages.stream().filter(p -> p.id().equalsIgnoreCase(id)).findFirst();
    }
}
