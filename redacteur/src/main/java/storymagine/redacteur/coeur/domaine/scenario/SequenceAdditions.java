package storymagine.redacteur.coeur.domaine.scenario;

import storymagine.redacteur.coeur.domaine.scenario.focus.FocusItem;
import storymagine.redacteur.coeur.domaine.scenario.lore.LoreItem;
import storymagine.redacteur.coeur.domaine.scenario.personnage.Personnage;

import java.util.List;

/** Additive elements declared on a sequence, merged on top of ChapterDefaults. */
public class SequenceAdditions {

    public static final SequenceAdditions NONE =
            new SequenceAdditions(List.of(), List.of(), List.of(), RequirementList.EMPTY);

    private final List<Personnage> characters;
    private final List<FocusItem>  focus;
    private final List<LoreItem>   lore;
    private final RequirementList  requirements;

    public SequenceAdditions(List<Personnage> characters, List<FocusItem> focus,
                             List<LoreItem> lore, RequirementList requirements) {
        this.characters   = List.copyOf(characters);
        this.focus        = List.copyOf(focus);
        this.lore         = List.copyOf(lore);
        this.requirements = requirements != null ? requirements : RequirementList.EMPTY;
    }

    public List<Personnage> characters()   { return characters; }
    public List<FocusItem>  focus()        { return focus; }
    public List<LoreItem>   lore()         { return lore; }
    public RequirementList  requirements() { return requirements; }
}
