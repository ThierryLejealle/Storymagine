package storymagine.testllm.coeur.domaine;

import java.util.Arrays;

/**
 * Résultat d'une passe de benchmark pour un modèle donné.
 */
public record BenchPasseResult(
        String   modele,
        boolean  think,
        boolean  probeOk,
        long[]   elapsedMs,
        int[]    responseToks,
        double[] tps,
        long     avgMemMb,
        int      ctx,
        int      maxCtx,
        boolean  divergence) {

    public BenchPasseResult(String modele, boolean think, boolean probeOk,
                            long[] elapsedMs, int[] responseToks, double[] tps) {
        this(modele, think, probeOk, elapsedMs, responseToks, tps, -1L, 0, 0, false);
    }

    public BenchPasseResult withMemMb(long memMb) {
        return new BenchPasseResult(modele, think, probeOk, elapsedMs, responseToks, tps,
                memMb, ctx, maxCtx, divergence);
    }

    public BenchPasseResult withCtx(int contextSize, int modelMaxCtx) {
        return new BenchPasseResult(modele, think, probeOk, elapsedMs, responseToks, tps,
                avgMemMb, contextSize, modelMaxCtx, divergence);
    }

    public int    successCount() { return elapsedMs.length; }
    public long   minMs()        { return Arrays.stream(elapsedMs).min().orElse(0); }
    public long   avgMs()        { return (long) Arrays.stream(elapsedMs).average().orElse(0); }
    public long   maxMs()        { return Arrays.stream(elapsedMs).max().orElse(0); }
    public int    minTok()       { return Arrays.stream(responseToks).min().orElse(0); }
    public int    avgTok()       { return (int) Arrays.stream(responseToks).average().orElse(0); }
    public int    maxTok()       { return Arrays.stream(responseToks).max().orElse(0); }
    public double avgTps()       { return Arrays.stream(tps).average().orElse(0); }
    public String ctxLabel()     { return ctx    > 0 ? (ctx    / 1024) + "k" : "-"; }
    public String maxCtxLabel()  { return maxCtx > 0 ? (maxCtx / 1024) + "k" : "-"; }
}
