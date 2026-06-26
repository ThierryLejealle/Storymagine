package storymagine.commun.infra;

import storymagine.commun.coeur.ports.LogPort;

import java.lang.management.ManagementFactory;

/**
 * Configuration de l'adaptateur Ollama et fabrique d'instances OllamaAdapter.
 */
public record OllamaConfig(
    String      baseUrl,
    int         contextWindowSize,
    int         maxContextWindowSize,
    int         topK,
    double      topP,
    double      repeatPenalty,
    int         numPredict,
    boolean     streamMode,
    int         timeoutMs,              // mode sync : timeout total de la requête
    int         firstTokenTimeoutMs,    // mode stream : chargement modèle + prefill → premier token
    int         interTokenTimeoutMs,    // mode stream : délai max entre deux tokens consécutifs
    RetryPolicy retryPolicy,
    int         largeModelRamFractionPct,
    int         largeModelTimeoutMultiplier // mode sync uniquement
) {

    public OllamaAdapter adapter(String model) {
        return adapter(model, false, LogPort.NOOP);
    }

    public OllamaAdapter adapter(String model, LogPort log) {
        return adapter(model, false, log);
    }

    public OllamaAdapter adapter(String model, boolean think) {
        return adapter(model, think, LogPort.NOOP);
    }

    public OllamaAdapter adapter(String model, boolean think, LogPort log) {
        return new OllamaAdapter(baseUrl, model, contextWindowSize, maxContextWindowSize,
            topK, topP, repeatPenalty, numPredict,
            streamMode, timeoutMs, firstTokenTimeoutMs, interTokenTimeoutMs,
            think, retryPolicy, log);
    }

    /** Crée un adaptateur avec timeout sync ajusté selon la taille du modèle (stream mode : sans effet). */
    public OllamaAdapter adapter(String model, int ctx, long sizeBytes, boolean think, LogPort log) {
        int effectiveSyncTimeout = isLargeModel(sizeBytes) ? timeoutMs * largeModelTimeoutMultiplier : timeoutMs;
        return new OllamaAdapter(baseUrl, model, ctx, maxContextWindowSize,
            topK, topP, repeatPenalty, numPredict,
            streamMode, effectiveSyncTimeout, firstTokenTimeoutMs, interTokenTimeoutMs,
            think, retryPolicy, log);
    }

    /** Crée un adaptateur avec timeout sync ajusté selon la taille du modèle (sans log). */
    public OllamaAdapter adapter(String model, int ctx, long sizeBytes, boolean think) {
        return adapter(model, ctx, sizeBytes, think, LogPort.NOOP);
    }

    public boolean isLargeModel(long sizeBytes) {
        return sizeBytes > 0 && sizeBytes > largeModelThresholdBytes();
    }

    public long largeModelThresholdBytes() {
        return systemRamBytes() * largeModelRamFractionPct / 100;
    }

    public long systemRamBytes() {
        var raw = ManagementFactory.getOperatingSystemMXBean();
        if (raw instanceof com.sun.management.OperatingSystemMXBean os) {
            return os.getTotalMemorySize();
        }
        return 32L * 1_073_741_824L;
    }
}
