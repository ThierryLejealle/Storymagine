package storymagine.testllm.coeur.domaine;

/**
 * Résultat agrégé d'un modèle sur l'ensemble des passes d'un benchmark.
 */
public record BenchModeleResult(
        String        modele,
        boolean       think,
        String        famille,
        String        paramSize,
        String        quantization,
        int           ctx,
        int           maxCtx,
        long          avgMemMb,
        double        globalAvgTps,
        boolean       divergence,
        HardwareUsage avgHardware) {}
