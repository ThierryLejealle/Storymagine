package storymagine.redacteur.coeur.domaine.orchestrator;

/**
 * Generation quality preset — controls which agents run and how many retries are allowed.
 * Adding a new level requires only a new enum constant; no workflow code changes needed.
 */
public enum QualityLevel {

    //                      write  planCr proof  seqCk  eval  pRtry sRtry cRtry thresh elimin corrRpt planThresh planElimin bookCr bRtry bookThresh bookElimin
    /** Plan only — writing phases skipped. */
    PLAN_ONLY              (false, false, false, false, false,   0,    0,    0,   7.0,  3.0,  false,   7.0,      3.0,      false, 0,    1.0,       0.0),

    /** Plan + write, minimum agents (StateExtractor + repetition memory only). */
    BROUILLON              (true,  false, false, false, false,   0,    0,    0,   8.0,  1.0,  false,   8.0,      1.0,      false, 0,    1.0,       0.0),

    /** Plan critics + write + sequence checkers + chapter critics, 1 retry each. */
    SIMPLE                 (true,  true,  true,  true,  false,   1,    1,    1,   7.0,  3.0,  false,   7.0,      3.0,      false, 0,    1.0,       0.0),

    /**
     * Full pipeline: SIMPLE + global evaluation, 3 plan retries and 2 sequence/chapter retries.
     * Book-level critique (NarrativeArcAnalyzer + CausalAnalyzer) kept deliberately lenient
     * (bookAverageThreshold=1.0, bookEliminationThreshold=0.0) while these two agents are not
     * yet well tested — see evols/2026-07-07-2149-story-plan-workflow.md.
     */
    FULL                   (true,  true,  true,  true,  true,    3,    2,    2,   7.0,  5.0,  true,    8.0,      5.5,      true,  2,    1.0,       0.0);

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
    private final double  eliminationThreshold;
    private final boolean runsCorrectorsRepeat;
    private final double  planAverageThreshold;
    private final double  planEliminationThreshold;
    private final boolean runsBookCritics;
    private final int     bookMaxRetry;
    private final double  bookAverageThreshold;
    private final double  bookEliminationThreshold;

    QualityLevel(boolean runsWriting,
                 boolean runsPlanCritics,
                 boolean runsProofreader,
                 boolean runsSequenceCheckers,
                 boolean runsEvaluation,
                 int planMaxRetry,
                 int sequenceMaxRetry,
                 int chapitreMaxRetry,
                 double chapitreThreshold,
                 double eliminationThreshold,
                 boolean runsCorrectorsRepeat,
                 double planAverageThreshold,
                 double planEliminationThreshold,
                 boolean runsBookCritics,
                 int bookMaxRetry,
                 double bookAverageThreshold,
                 double bookEliminationThreshold) {
        this.runsWriting               = runsWriting;
        this.runsPlanCritics           = runsPlanCritics;
        this.runsProofreader           = runsProofreader;
        this.runsSequenceCheckers      = runsSequenceCheckers;
        this.runsEvaluation            = runsEvaluation;
        this.planMaxRetry              = planMaxRetry;
        this.sequenceMaxRetry          = sequenceMaxRetry;
        this.chapitreMaxRetry          = chapitreMaxRetry;
        this.chapitreThreshold         = chapitreThreshold;
        this.eliminationThreshold      = eliminationThreshold;
        this.runsCorrectorsRepeat      = runsCorrectorsRepeat;
        this.planAverageThreshold      = planAverageThreshold;
        this.planEliminationThreshold  = planEliminationThreshold;
        this.runsBookCritics           = runsBookCritics;
        this.bookMaxRetry              = bookMaxRetry;
        this.bookAverageThreshold      = bookAverageThreshold;
        this.bookEliminationThreshold  = bookEliminationThreshold;
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    public boolean runsWriting()               { return runsWriting; }
    public boolean runsPlanCritics()           { return runsPlanCritics; }
    public boolean runsProofreader()           { return runsProofreader; }
    public boolean runsSequenceCheckers()      { return runsSequenceCheckers; }
    public boolean runsEvaluation()            { return runsEvaluation; }
    public int     planMaxRetry()              { return planMaxRetry; }
    public int     sequenceMaxRetry()          { return sequenceMaxRetry; }
    public int     chapitreMaxRetry()          { return chapitreMaxRetry; }
    /** Average-score threshold for the write phase (sequence + chapter critics). */
    public double  chapitreThreshold()         { return chapitreThreshold; }
    /** Elimination threshold for the write phase (sequence + chapter critics). */
    public double  eliminationThreshold()      { return eliminationThreshold; }
    public boolean runsCorrectorsRepeat()      { return runsCorrectorsRepeat; }
    /** Average-score threshold for the plan phase. */
    public double  planAverageThreshold()      { return planAverageThreshold; }
    /** Elimination threshold for the plan phase. */
    public double  planEliminationThreshold()  { return planEliminationThreshold; }
    /** Whether the whole-book critique (NarrativeArcAnalyzer + CausalAnalyzer) runs at all. */
    public boolean runsBookCritics()           { return runsBookCritics; }
    public int     bookMaxRetry()              { return bookMaxRetry; }
    /** Average-score threshold for the book-level critique. */
    public double  bookAverageThreshold()      { return bookAverageThreshold; }
    /** Elimination threshold for the book-level critique. */
    public double  bookEliminationThreshold()  { return bookEliminationThreshold; }
}
