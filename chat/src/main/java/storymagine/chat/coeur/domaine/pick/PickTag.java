package storymagine.chat.coeur.domaine.pick;

/**
 * The name of a pick library, as written in a "{pick mode name}" command : also the file name (without
 * ".txt") inside the scenario's picks/ directory. Letters, digits, '_' and '-' only, so a tag can
 * never point outside picks/.
 */
public record PickTag(String name) {

    public PickTag {
        if (name == null || !name.matches("[\\p{L}\\p{N}_-]+")) {
            throw new IllegalArgumentException("Nom de pick invalide : " + name);
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
