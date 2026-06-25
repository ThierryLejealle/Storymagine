package storymagine.redacteur.coeur.domaine.orchestrator;

/**
 * Generation quality preset — controls which agents run and how many retries are allowed.
 * Adding a new level requires only a new enum constant; no workflow code changes needed.
 */
public enum QualityLevel {

    //                      write  planCr proof  seqCk  eval  pRtry sRtry cRtry thresh
    /** Plan only — writing phases skipped. */
    PLAN_ONLY              (false, false, false, false, false,   0,    0,    0,   7.0),

    /** Plan + write, minimum agents (StateExtractor + repetition memory only). */
    BROUILLON              (true,  false, false, false, false,   0,    0,    0,   7.0),

    /** Plan critics + write + sequence checkers + chapter critics, 1 retry each. */
    SIMPLE                 (true,  true,  true,  true,  false,   1,    1,    1,   7.0),

    /** Full pipeline: SIMPLE + global evaluation, 4 plan retries and 2 sequence/chapter retries. */
    FULL                   (true,  true,  true,  true,  true,    4,    2,    2,   7.0);

    // ── Fields ──────────────────────────────────────────────────────────────

    private final boolean runsWriting;
    private final boolean runsPlanCritics;
    private final boolean runsProofreader;
    private final boolean runsSequenceCheckers;
    private final boolean runsEvaluation;
    private final int     planMaxRetry;
    private final int     sequenceMaxRetry;
    private final int     chapitreMaxRetry;
    private final double  chapitreThreshold;

    QualityLevel(boolean runsWriting,
                 boolean runsPlanCritics,
                 boolean runsProofreader,
                 boolean runsSequenceCheckers,
                 boolean runsEvaluation,
                 int planMaxRetry,
                 int sequenceMaxRetry,
                 int chapitreMaxRetry,
                 double chapitreThreshold) {
        this.runsWriting           = runsWriting;
        this.runsPlanCritics       = runsPlanCritics;
        this.runsProofreader       = runsProofreader;
        this.runsSequenceCheckers  = runsSequenceCheckers;
        this.runsEvaluation        = runsEvaluation;
        this.planMaxRetry          = planMaxRetry;
        this.sequenceMaxRetry      = sequenceMaxRetry;
        this.chapitreMaxRetry      = chapitreMaxRetry;
        this.chapitreThreshold     = chapitreThreshold;
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    public boolean runsWriting()           { return runsWriting; }
    public boolean runsPlanCritics()       { return runsPlanCritics; }
    public boolean runsProofreader()       { return runsProofreader; }
    public boolean runsSequenceCheckers()  { return runsSequenceCheckers; }
    public boolean runsEvaluation()        { return runsEvaluation; }
    public int     planMaxRetry()          { return planMaxRetry; }
    public int     sequenceMaxRetry()      { return sequenceMaxRetry; }
    public int     chapitreMaxRetry()      { return chapitreMaxRetry; }
    public double  chapitreThreshold()     { return chapitreThreshold; }
}
