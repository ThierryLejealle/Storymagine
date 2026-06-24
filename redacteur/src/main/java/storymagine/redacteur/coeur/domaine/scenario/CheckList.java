package storymagine.redacteur.coeur.domaine.scenario;

import java.util.List;

/** Global checks from checks.md, split by audience. */
public class CheckList {

    public static final CheckList EMPTY = new CheckList(List.of(), List.of());

    private final List<Check> planChecks;
    private final List<Check> writerChecks;

    public CheckList(List<Check> planChecks, List<Check> writerChecks) {
        this.planChecks   = List.copyOf(planChecks);
        this.writerChecks = List.copyOf(writerChecks);
    }

    public List<Check> planChecks()   { return planChecks; }
    public List<Check> writerChecks() { return writerChecks; }
}
