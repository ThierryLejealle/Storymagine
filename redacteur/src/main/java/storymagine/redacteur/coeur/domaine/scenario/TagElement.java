package storymagine.redacteur.coeur.domaine.scenario;

/** Base for any named narrative element that can carry PLAN and WRITER variants. */
public abstract class TagElement {

    private final String tag;
    private final String globalContent;
    private final String planContent;
    private final String writerContent;

    protected TagElement(String tag, String globalContent, String planContent, String writerContent) {
        this.tag = tag;
        this.globalContent = globalContent;
        this.planContent = planContent;
        this.writerContent = writerContent;
    }

    public String tag()           { return tag; }
    public String globalContent() { return globalContent; }
    public String planContent()   { return planContent; }
    public String writerContent() { return writerContent; }
}
