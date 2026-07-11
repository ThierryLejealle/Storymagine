package storymagine.redacteur.coeur.domaine.agent.commun;

/** How a Corrector's self-repeat loop (retry on its own already-patched output) decides to continue. */
public enum RetryStrategy {

    /** Never repeats — a single pass, whatever it finds. */
    SINGLE_PASS,

    /** Repeats while the corrections/word ratio or absolute count exceeds a fixed threshold. */
    RATIO_THRESHOLD,

    /** Repeats while the finding count is still decreasing from the previous pass. */
    DECREASING,

    /** Repeats only while BOTH RATIO_THRESHOLD and DECREASING hold. */
    DECREASING_AND_RATIO_THRESHOLD
}
