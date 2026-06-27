package storymagine.redacteur.coeur.domaine.orchestrator.write;

/** Configures corrector re-run behaviour: ratio of corrections per word above which a second pass is triggered. */
public record CorrectorConfig(float repeatThresholdPerWord) {

    public static CorrectorConfig defaults() {
        return new CorrectorConfig(0.016f);
    }
}
