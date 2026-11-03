package storymagine.chat.coeur.domaine.pick;

import java.util.regex.Pattern;

/**
 * The name of a pick library, as written in a "{pick mode name}" command. Two kinds : a scenario's own
 * library, "name", the file name (without ".txt") inside its picks/ directory, and a library shared by
 * several scenarios, "/lib/name" (or "/lib/folder/name"), a file of the lib/ directory next to the scenarios.
 * Letters, digits, '_' and '-' only in each part (no '.'), so a tag can never point outside picks/ or lib/.
 */
public record PickTag(String name) {

    public static final String SHARED_PREFIX = "/lib/";
    private static final Pattern VALID = Pattern.compile("[\\p{L}\\p{N}_-]+|/lib(/[\\p{L}\\p{N}_-]+)+");

    public PickTag {
        if (name == null || !VALID.matcher(name).matches()) {
            throw new IllegalArgumentException("Nom de pick invalide : " + name
                + " (un nom, ou /lib/nom pour une bibliotheque commune : lettres, chiffres, _ et - seulement)");
        }
    }

    /** True for "/lib/..." : a library shared by several scenarios. */
    public boolean isShared() {
        return name.startsWith(SHARED_PREFIX);
    }

    /** The path inside lib/, without ".txt" ("lieux/ville" for "/lib/lieux/ville"). Only for a shared tag. */
    public String sharedPath() {
        return name.substring(SHARED_PREFIX.length());
    }

    /** The file this tag stands for, as an author writes it in a message : "picks/lieu.txt" or "lib/lieux/ville.txt". */
    public String file() {
        return isShared() ? "lib/" + sharedPath() + ".txt" : "picks/" + name + ".txt";
    }

    @Override
    public String toString() {
        return name;
    }
}
