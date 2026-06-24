package storymagine.redacteur.coeur.domaine.orchestrator;

/** Runtime configuration for a story generation run. Retry counts come from {@link EngineConfig}. */
public record GenerationConfig(GenerationMode mode, boolean jsonMode) {

    public static GenerationConfig draft() {
        return new GenerationConfig(GenerationMode.DRAFT, false);
    }

    public static GenerationConfig standard() {
        return new GenerationConfig(GenerationMode.STANDARD, true);
    }

    public static GenerationConfig full() {
        return new GenerationConfig(GenerationMode.FULL, true);
    }
}
