package storymagine.redacteur.coeur.domaine.scenario;

/** Global parameters from scenario.md. */
public class ScenarioConfig {

    private final String title;
    private final String language;
    private final int    defaultSequenceWords;
    private final int    contextWindow;
    private final String stitch;

    public ScenarioConfig(String title, String language, int defaultSequenceWords,
                          int contextWindow, String stitch) {
        this.title                = title;
        this.language             = language;
        this.defaultSequenceWords = defaultSequenceWords;
        this.contextWindow        = contextWindow;
        this.stitch               = stitch;
    }

    public String title()                { return title; }
    public String language()             { return language; }
    public int    defaultSequenceWords() { return defaultSequenceWords; }
    public int    contextWindow()        { return contextWindow; }
    /** Global stitch rule for opening lines — null if not set. */
    public String stitch()               { return stitch; }
}
