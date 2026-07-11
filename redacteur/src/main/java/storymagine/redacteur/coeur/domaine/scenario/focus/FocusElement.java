package storymagine.redacteur.coeur.domaine.scenario.focus;

import storymagine.redacteur.coeur.domaine.scenario.TagElement;

/** A named focus element defined in focus.md and referenceable by tag from chapters. */
public class FocusElement extends TagElement {

    /** Optional — the check given to Critics verifying this focus was used. Null if absent (# CHECK section not declared). */
    private final String checkContent;

    public FocusElement(String tag, String globalContent, String planContent, String writerContent, String checkContent) {
        super(tag, globalContent, planContent, writerContent);
        this.checkContent = checkContent;
    }

    public String checkContent() { return checkContent; }
}
