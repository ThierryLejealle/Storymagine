package storymagine.redacteur.coeur.domaine.scenario;

import storymagine.redacteur.coeur.domaine.scenario.focus.FocusPool;
import storymagine.redacteur.coeur.domaine.scenario.lore.LorePool;
import storymagine.redacteur.coeur.domaine.scenario.personnage.PersonnagePool;

import java.util.List;

/** Root aggregate: a fully loaded scenario with all its pools and chapters. */
public class Scenario {

    private final ScenarioConfig   config;
    private final String           bookGoal;
    private final String           quality;
    private final String           writingStyle;
    private final String           keepPhrases;
    private final String           writingExample;
    private final PersonnagePool   personnages;
    private final FocusPool        focus;
    private final LorePool         lore;
    private final CheckList        checks;
    private final ConstraintList   constraints;
    private final List<Chapter>    chapters;

    public Scenario(ScenarioConfig config,
                    String bookGoal, String quality, String writingStyle, String keepPhrases,
                    String writingExample,
                    PersonnagePool personnages, FocusPool focus, LorePool lore,
                    CheckList checks, ConstraintList constraints, List<Chapter> chapters) {
        this.config         = config;
        this.bookGoal       = bookGoal;
        this.quality        = quality;
        this.writingStyle   = writingStyle;
        this.keepPhrases    = keepPhrases;
        this.writingExample = writingExample;
        this.personnages    = personnages;
        this.focus          = focus;
        this.lore           = lore;
        this.checks         = checks;
        this.constraints    = constraints;
        this.chapters       = List.copyOf(chapters);
    }

    public ScenarioConfig  config()         { return config; }
    /** Narrative goal from goal.md — null if file absent. Falls back to title in formatters. */
    public String          bookGoal()       { return bookGoal; }
    /** Quality criteria from quality.md — null if file absent. */
    public String          quality()        { return quality; }
    /** Writing style from style.md — null if file absent. */
    public String          writingStyle()   { return writingStyle; }
    /** Allowed recurring motifs from keep_phrases.md — null if file absent or empty. */
    public String          keepPhrases()    { return keepPhrases; }
    public String          writingExample() { return writingExample; }
    public PersonnagePool  personnages()    { return personnages; }
    public FocusPool       focus()          { return focus; }
    public LorePool        lore()           { return lore; }
    public CheckList       checks()         { return checks; }
    public ConstraintList  constraints()    { return constraints; }
    public List<Chapter>   chapters()       { return chapters; }
}
