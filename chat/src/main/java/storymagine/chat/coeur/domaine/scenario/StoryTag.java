package storymagine.chat.coeur.domaine.scenario;

/**
 * A "## tag" section of a character sheet : text that is NOT part of the sheet until a
 * "{npc-story on Name tag}" switches it on (see StoryDirective), and that a later "off" removes
 * again. name is the first word of the heading (the rest of the line is a comment for the author).
 * visibility comes from the place of the section in the file, not from its text : before the
 * "# SECRET" line it is PUBLIC (once on, the other characters read it too), after it SECRET (only
 * this character reads it) — and it never changes camp.
 */
public record StoryTag(String name, CharacterDelta.Visibility visibility, String text) {

    public boolean isNamed(String wanted) {
        return name.equalsIgnoreCase(wanted == null ? "" : wanted.strip());
    }
}
