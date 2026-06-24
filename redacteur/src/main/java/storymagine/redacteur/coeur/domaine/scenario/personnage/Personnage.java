package storymagine.redacteur.coeur.domaine.scenario.personnage;

import storymagine.redacteur.coeur.domaine.scenario.TagElement;

/**
 * Character sheet loaded from characters/{id}.md.
 * tag          = file id (pierre_moreau)
 * globalContent = ## GENERAL section
 * planContent   = ## INTRIGUE + private + unconscious sections
 * writerContent = ## DESCRIPTION section
 */
public class Personnage extends TagElement {

    public Personnage(String id, String generalContent, String planContent, String descriptionContent) {
        super(id, generalContent, planContent, descriptionContent);
    }

    /** The character's file id, e.g. "pierre_moreau". */
    public String id() {
        return tag();
    }
}
