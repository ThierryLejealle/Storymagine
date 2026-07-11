package storymagine.redacteur.coeur.domaine.scenario;

import storymagine.redacteur.coeur.domaine.scenario.focus.FocusItem;
import storymagine.redacteur.coeur.domaine.scenario.lore.LoreItem;
import storymagine.redacteur.coeur.domaine.scenario.personnage.Personnage;

import java.util.List;

/** Default values inherited by all sequences in a chapter. */
public class ChapterDefaults {

    public static final ChapterDefaults EMPTY =
            new ChapterDefaults(List.of(), List.of(), List.of(), RequirementList.EMPTY, null, null);

    private final List<Personnage> characters;
    private final List<FocusItem>  focus;
    private final List<LoreItem>   lore;
    private final RequirementList  requirements;
    private final Integer          plannerEffortInLines;
    private final Integer          sequenceMinWords;

    public ChapterDefaults(List<Personnage> characters, List<FocusItem> focus,
                           List<LoreItem> lore, RequirementList requirements,
                           Integer plannerEffortInLines, Integer sequenceMinWords) {
        this.characters           = List.copyOf(characters);
        this.focus                = List.copyOf(focus);
        this.lore                 = List.copyOf(lore);
        this.requirements         = requirements != null ? requirements : RequirementList.EMPTY;
        this.plannerEffortInLines = plannerEffortInLines;
        this.sequenceMinWords     = sequenceMinWords;
    }

    public List<Personnage> characters()         { return characters; }
    public List<FocusItem>  focus()              { return focus; }
    public List<LoreItem>   lore()               { return lore; }
    public RequirementList  requirements()       { return requirements; }
    /** Minimum beats per sequence override for this chapter — null means inherit from scenario or agent default. */
    public Integer          plannerEffortInLines() { return plannerEffortInLines; }
    /** Target word count per sequence for this chapter — null means inherit from scenario config. */
    public Integer          sequenceMinWords()     { return sequenceMinWords; }
}
