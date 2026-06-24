package storymagine.redacteur.coeur.domaine.scenario;

import java.util.List;

/** Global constraints from constraints.md, split by audience. */
public class ConstraintList {

    public static final ConstraintList EMPTY = new ConstraintList(List.of(), List.of());

    private final List<Constraint> planConstraints;
    private final List<Constraint> writerConstraints;

    public ConstraintList(List<Constraint> planConstraints, List<Constraint> writerConstraints) {
        this.planConstraints   = List.copyOf(planConstraints);
        this.writerConstraints = List.copyOf(writerConstraints);
    }

    public List<Constraint> planConstraints()   { return planConstraints; }
    public List<Constraint> writerConstraints() { return writerConstraints; }
}
