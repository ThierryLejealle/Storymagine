package storymagine.redacteur.coeur.domaine.scenario;

/** A single narrative sequence within a chapter. */
public class Sequence {

    private final NarrativeType      type;
    private final String             directive;
    private final boolean            noTransition;
    private final SequenceOverrides  overrides;
    private final SequenceAdditions  additions;

    public Sequence(NarrativeType type, String directive, boolean noTransition,
                    SequenceOverrides overrides, SequenceAdditions additions) {
        this.type         = type;
        this.directive    = directive;
        this.noTransition = noTransition;
        this.overrides    = overrides  != null ? overrides  : SequenceOverrides.NONE;
        this.additions    = additions  != null ? additions  : SequenceAdditions.NONE;
    }

    public NarrativeType     type()         { return type; }
    public String            directive()    { return directive; }
    public boolean           noTransition() { return noTransition; }
    public SequenceOverrides overrides()    { return overrides; }
    public SequenceAdditions additions()    { return additions; }
}
