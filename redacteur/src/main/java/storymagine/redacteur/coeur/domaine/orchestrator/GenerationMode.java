package storymagine.redacteur.coeur.domaine.orchestrator;

public enum GenerationMode {

    /** Plan only — writing phases skipped. */
    PLAN_ONLY,

    /** Plan + write, no quality checks. */
    DRAFT,

    /** Plan + write + sequence-level critics and checkers. */
    STANDARD,

    /** Full pipeline: plan + write + checkers + global chapter evaluation. */
    FULL;

    public boolean runsPlanCritics()       { return this == STANDARD || this == FULL; }
    public boolean runsSequenceCheckers()  { return this == STANDARD || this == FULL; }
    public boolean runsEvaluation()        { return this == FULL; }
}
