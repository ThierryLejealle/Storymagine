package storymagine.commun.infra;

/**
 * Politique de reprise sur erreur réseau pour les appels Ollama.
 */
public record RetryPolicy(int retryCount, int delay1Seconds, int delay2Seconds, int delay3PlusSeconds) {

    public static final RetryPolicy DEFAULT = new RetryPolicy(5, 15, 30, 60);

    /** Délai en secondes avant la tentative numéro {@code retryNumber} (base 1). */
    public int delaySeconds(int retryNumber) {
        if (retryNumber == 1) return delay1Seconds;
        if (retryNumber == 2) return delay2Seconds;
        return delay3PlusSeconds;
    }
}
