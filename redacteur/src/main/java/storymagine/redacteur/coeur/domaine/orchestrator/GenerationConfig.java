package storymagine.redacteur.coeur.domaine.orchestrator;

/** Runtime configuration for a story generation run. */
public record GenerationConfig(QualityLevel qualityLevel, boolean jsonMode) {

    public static GenerationConfig planOnly() {
        return new GenerationConfig(QualityLevel.PLAN_ONLY, false);
    }

    public static GenerationConfig brouillon() {
        return new GenerationConfig(QualityLevel.BROUILLON, false);
    }

    public static GenerationConfig simple() {
        return new GenerationConfig(QualityLevel.SIMPLE, true);
    }

    public static GenerationConfig full() {
        return new GenerationConfig(QualityLevel.FULL, true);
    }
}
