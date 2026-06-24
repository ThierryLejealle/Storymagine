package storymagine.redacteur.coeur.domaine.orchestrator;

/**
 * Tuning parameters for critique and retry behaviour, loaded from engine.properties.
 * All values have sensible defaults (see {@link #defaults()}).
 */
public record EngineConfig(
    int    planMaxRetry,
    int    chapitreMaxRetry,
    double chapitreThreshold,
    int    sequenceMaxRetry
) {
    /** Production defaults — matches engine.properties. */
    public static EngineConfig defaults() {
        return new EngineConfig(3, 3, 7.0, 1);
    }
}
