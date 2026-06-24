package storymagine.redacteur.coeur.domaine.scenario;

import java.util.List;

/** A chapter with its sequences and inherited defaults. */
public class Chapter {

    private final String          comment;
    private final String          title;
    private final NarrativeType   type;
    private final String          description;
    private final String          setting;
    private final String          goal;
    private final ChapterDefaults defaults;
    private final List<Sequence>  sequences;

    public Chapter(String comment, String title, NarrativeType type, String description,
                   String setting, String goal, ChapterDefaults defaults, List<Sequence> sequences) {
        this.comment     = comment;
        this.title       = title;
        this.type        = type;
        this.description = description;
        this.setting     = setting;
        this.goal        = goal;
        this.defaults    = defaults != null ? defaults : ChapterDefaults.EMPTY;
        this.sequences   = List.copyOf(sequences);
    }

    /** Author note — not used in generation. Null if absent. */
    public String          comment()     { return comment; }
    public String          title()       { return title; }
    public NarrativeType   type()        { return type; }
    public String          description() { return description; }
    /** Brief spatio-temporal indication injected into prompts. Null if absent. */
    public String          setting()     { return setting; }
    public String          goal()        { return goal; }
    public ChapterDefaults defaults()    { return defaults; }
    public List<Sequence>  sequences()   { return sequences; }
}
