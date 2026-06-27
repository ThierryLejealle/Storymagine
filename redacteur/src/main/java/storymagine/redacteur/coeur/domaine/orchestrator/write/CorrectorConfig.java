package storymagine.redacteur.coeur.domaine.orchestrator.write;

/**
 * Configures corrector re-run behaviour.
 * Retry triggers when: ratio of corrections/word exceeds threshold OR absolute count >= minCorrectionsForRetry.
 */
public record CorrectorConfig(float repeatThresholdPerWord, int minCorrectionsForRetry, int maxRetryPasses) {

    public static CorrectorConfig defaults() {
        return new CorrectorConfig(0.010f, 7, 2);
    }
}
