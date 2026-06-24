package storymagine.redacteur.coeur.domaine.story;

/** Business identifier for a chapter within a story. */
public record ChapterId(String value) {

    public ChapterId {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("ChapterId cannot be blank");
    }

    @Override
    public String toString() { return value; }
}
