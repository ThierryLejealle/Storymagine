package storymagine.redacteur.coeur.domaine.scenario;

/** Global parameters from scenario.md. */
public class ScenarioConfig {

    private final String  title;
    private final String  language;
    private final int     defaultSequenceWords;
    private final int     contextWindow;
    private final String  stitch;
    private final Integer plannerEffortInLines;

    public ScenarioConfig(String title, String language, int defaultSequenceWords,
                          int contextWindow, String stitch, Integer plannerEffortInLines) {
        this.title                = title;
        this.language             = language;
        this.defaultSequenceWords = defaultSequenceWords;
        this.contextWindow        = contextWindow;
        this.stitch               = stitch;
        this.plannerEffortInLines = plannerEffortInLines;
    }

    public String  title()                { return title; }
    public String  language()             { return language; }
    public int     defaultSequenceWords() { return defaultSequenceWords; }
    public int     contextWindow()        { return contextWindow; }
    /** Global stitch rule for opening lines — null if not set. */
    public String  stitch()               { return stitch; }
    /** Minimum beats per sequence for the planner — null means use agent default. */
    public Integer plannerEffortInLines() { return plannerEffortInLines; }
}
