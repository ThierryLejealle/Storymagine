package storymagine.redacteur.coeur.domaine.scenario.lore;

import storymagine.redacteur.coeur.domaine.scenario.TagElement;

/** A named lore element defined in lore.md and referenceable by tag from chapters. */
public class LoreElement extends TagElement {

    public LoreElement(String tag, String globalContent, String planContent, String writerContent) {
        super(tag, globalContent, planContent, writerContent);
    }
}
