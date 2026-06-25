package storymagine.redacteur.coeur.domaine.orchestrator.plan;

/** Configures beat-count guidance for the planner: base offset, words-per-beat ratio, and tolerance margin. */
public record BeatsConfig(int beatsBase, int wordsPerBeat, int tolerancePct) {

    public static BeatsConfig defaults() {
        return new BeatsConfig(2, 75, 20);
    }
}
