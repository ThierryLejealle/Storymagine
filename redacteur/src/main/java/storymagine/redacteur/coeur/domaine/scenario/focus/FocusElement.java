package storymagine.redacteur.coeur.domaine.scenario.focus;

import storymagine.redacteur.coeur.domaine.scenario.TagElement;

/** A named focus element defined in focus.md and referenceable by tag from chapters. */
public class FocusElement extends TagElement {

    public FocusElement(String tag, String globalContent, String planContent, String writerContent) {
        super(tag, globalContent, planContent, writerContent);
    }
}
