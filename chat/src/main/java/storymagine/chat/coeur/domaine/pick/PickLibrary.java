package storymagine.chat.coeur.domaine.pick;

/**
 * Gives the paragraphs behind a tag. Throws IllegalStateException (message meant for the author)
 * if the tag has no library or the library is empty.
 */
@FunctionalInterface
public interface PickLibrary {

    PickParagraphs paragraphs(PickTag tag);
}
