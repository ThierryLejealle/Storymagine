package storymagine.testllm.coeur.domaine;

/** Utilisation matérielle moyenne mesurée pendant le benchmark d'un modèle. -1 = non disponible. */
public record HardwareUsage(int gpu0Pct, int gpu1Pct, int cpuPct) {
    public static final HardwareUsage ABSENT = new HardwareUsage(-1, -1, -1);
}
