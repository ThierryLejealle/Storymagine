package storymagine.testllm.coeur.domaine;

import storymagine.commun.coeur.ports.LlmResult;
import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.commun.coeur.ports.ModelEntry;
import storymagine.commun.coeur.ports.ModelLifecyclePort;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Orchestre l'exécution d'un benchmark sur une liste de modèles Ollama.
 * Sans logique d'affichage ni d'I/O fichier.
 */
public class BenchmarkRunner {

    private static final double DIVERGENCE_THRESHOLD = 0.50;

    public interface AdapterFactory {
        ModelCallPort      callPort(String model, int ctx, long sizeBytes, boolean think);
        ModelLifecyclePort lifecyclePort(String model, int ctx, long sizeBytes);
        int                detectContext(String model);
        boolean            detectThinking(String model);
        LongSupplier             vramSampler(String model);
        Supplier<HardwareUsage>  hardwareSampler();
        int                      configuredCtx();
        boolean                  isLargeModel(long sizeBytes);
    }

    public sealed interface BenchEvent permits
            BenchEvent.ModelStart, BenchEvent.ProbeOk, BenchEvent.ProbeFail,
            BenchEvent.RunOk, BenchEvent.RunFail, BenchEvent.Divergence,
            BenchEvent.PasseComplete, BenchEvent.IterationSummary,
            BenchEvent.PasseDone, BenchEvent.ModelUnload {

        record ModelStart(String model) implements BenchEvent {}
        record ProbeOk(ModelLifecyclePort info, int ctx, int maxCtx,
                       boolean isLarge, long sizeBytes, double elapsedSec) implements BenchEvent {}
        record ProbeFail(String model, double elapsedSec, String message) implements BenchEvent {}
        record RunOk(String passe, int run, int total,
                     int chars, double tps, double elapsedSec) implements BenchEvent {}
        record RunFail(String passe, int run, int total,
                       double elapsedSec, String message) implements BenchEvent {}
        record Divergence(String passe, String tokensResume) implements BenchEvent {}
        record PasseComplete(PasseBench passe, String model, boolean think,
                             List<Long> elapsed, List<Integer> toks,
                             List<Double> tps, List<String> texts) implements BenchEvent {}
        record IterationSummary(String model, boolean think,
                                List<BenchPasseResult> results,
                                List<PasseBench> passes) implements BenchEvent {}
        record PasseDone(String titre, List<BenchPasseResult> resultats) implements BenchEvent {}
        record ModelUnload(String model, HardwareUsage hardware) implements BenchEvent {}
    }

    @FunctionalInterface
    public interface RunLogger {
        void onEvent(BenchEvent event);
    }

    private final AdapterFactory factory;
    private final int            runs;
    private final int            maxModels;
    private final long           maxModelSizeBytes;

    private final List<BenchModeleResult>              modeleResults = new ArrayList<>();
    private final Map<Integer, List<BenchPasseResult>> parPasse      = new LinkedHashMap<>();

    public BenchmarkRunner(AdapterFactory factory, int runs, int maxModels, long maxModelSizeBytes) {
        this.factory           = factory;
        this.runs              = runs;
        this.maxModels         = maxModels;
        this.maxModelSizeBytes = maxModelSizeBytes;
    }

    public BenchRun run(List<ModelEntry> entries, List<PasseBench> passes, RunLogger log) {
        List<ModelEntry> modeles = filtrer(entries);
        return execute(modeles, passes, log);
    }

    /** Exécute le benchmark sans appliquer le filtre de taille — pour les listes déjà choisies explicitement. */
    public BenchRun runDirect(List<ModelEntry> entries, List<PasseBench> passes, RunLogger log) {
        List<ModelEntry> modeles = maxModels > 0 && entries.size() > maxModels
                ? entries.subList(0, maxModels)
                : new ArrayList<>(entries);
        return execute(modeles, passes, log);
    }

    private BenchRun execute(List<ModelEntry> modeles, List<PasseBench> passes, RunLogger log) {
        for (int p = 0; p < passes.size(); p++) parPasse.put(p, new ArrayList<>());

        Map<String, Boolean> thinkSupport = new LinkedHashMap<>();
        for (ModelEntry e : modeles) thinkSupport.put(e.name(), factory.detectThinking(e.name()));

        for (ModelEntry entry : modeles) {
            String        model     = entry.name();
            boolean       supThink  = thinkSupport.getOrDefault(model, false);
            List<Boolean> thinkVars = supThink ? List.of(false, true) : List.of(false);

            int maxCtx       = factory.detectContext(model);
            int effectiveCtx = Math.min(factory.configuredCtx(), maxCtx);

            log.onEvent(new BenchEvent.ModelStart(model));

            ModelLifecyclePort lifecycle = factory.lifecyclePort(model, effectiveCtx, entry.sizeBytes());
            ModelCallPort      baseCall  = factory.callPort(model, effectiveCtx, entry.sizeBytes(), false);

            long probeStart = System.currentTimeMillis();
            try {
                lifecycle.probe();
                double elapsedSec = (System.currentTimeMillis() - probeStart) / 1000.0;
                log.onEvent(new BenchEvent.ProbeOk(lifecycle, effectiveCtx, maxCtx,
                        factory.isLargeModel(entry.sizeBytes()), entry.sizeBytes(), elapsedSec));
            } catch (Exception e) {
                double elapsedSec = (System.currentTimeMillis() - probeStart) / 1000.0;
                log.onEvent(new BenchEvent.ProbeFail(model, elapsedSec, e.getMessage()));
                for (boolean think : thinkVars) {
                    for (int p = 0; p < passes.size(); p++) {
                        parPasse.get(p).add(new BenchPasseResult(model, think, false,
                                new long[0], new int[0], new double[0]));
                    }
                }
                continue;
            }

            MemorySampler         sampler   = new MemorySampler(factory.vramSampler(model));
            HardwareUsageSampler  hwSampler = new HardwareUsageSampler(factory.hardwareSampler());
            sampler.start();
            hwSampler.start();

            List<List<BenchPasseResult>> allIterResults = new ArrayList<>();

            for (boolean think : thinkVars) {
                ModelCallPort llm = think
                        ? factory.callPort(model, effectiveCtx, entry.sizeBytes(), true)
                        : baseCall;

                List<BenchPasseResult> passResults = new ArrayList<>();
                for (int p = 0; p < passes.size(); p++) {
                    passResults.add(runPasse(passes.get(p), model, think, llm, log));
                }
                allIterResults.add(passResults);
                log.onEvent(new BenchEvent.IterationSummary(model, think, passResults, passes));
            }

            sampler.stop();
            hwSampler.stop();
            long          avgMem = sampler.avgMb();
            HardwareUsage avgHw  = hwSampler.avg();

            for (int ii = 0; ii < thinkVars.size(); ii++) {
                boolean                think   = thinkVars.get(ii);
                List<BenchPasseResult> iterRes = allIterResults.get(ii);

                for (int p = 0; p < passes.size(); p++) {
                    parPasse.get(p).add(iterRes.get(p).withMemMb(avgMem).withCtx(effectiveCtx, maxCtx));
                }

                double  sumTps = 0; int cntTps = 0; boolean anyDiv = false;
                for (BenchPasseResult r : iterRes) {
                    if (r.probeOk() && r.successCount() > 0) { sumTps += r.avgTps(); cntTps++; }
                    if (r.divergence()) anyDiv = true;
                }
                modeleResults.add(new BenchModeleResult(
                        model, think,
                        lifecycle.modelFamily(), lifecycle.modelParamSize(), lifecycle.modelQuantization(),
                        effectiveCtx, maxCtx, avgMem,
                        cntTps > 0 ? sumTps / cntTps : 0.0,
                        anyDiv, avgHw));
            }

            log.onEvent(new BenchEvent.ModelUnload(model, avgHw));
            lifecycle.unload();
        }

        for (int p = 0; p < passes.size(); p++) {
            String titre = "Passe " + (p + 1) + "/" + passes.size() + " : " + passes.get(p).nom()
                    + "  (" + runs + " appels)";
            log.onEvent(new BenchEvent.PasseDone(titre, parPasse.get(p)));
        }

        return new BenchRun(List.copyOf(modeleResults), Map.copyOf(parPasse), passes);
    }

    private BenchPasseResult runPasse(PasseBench passe, String model, boolean think,
                                      ModelCallPort llm, RunLogger log) {
        List<Long>    elapsed = new ArrayList<>(runs);
        List<Integer> toks    = new ArrayList<>(runs);
        List<Double>  tpsList = new ArrayList<>(runs);
        List<String>  texts   = new ArrayList<>(runs);

        for (int i = 0; i < runs; i++) {
            long start = System.currentTimeMillis();
            try {
                LlmResult r  = llm.generate(passe.systemPrompt(), passe.userPrompt(), 0.7);
                long      ms = System.currentTimeMillis() - start;
                elapsed.add(ms);
                toks.add(r.text().length());
                tpsList.add(r.tokensPerSecond());
                texts.add(r.text());
                log.onEvent(new BenchEvent.RunOk(passe.nom(), i + 1, runs, r.text().length(), r.tokensPerSecond(), ms / 1000.0));
            } catch (Exception e) {
                long ms = System.currentTimeMillis() - start;
                texts.add("ERREUR : " + e.getMessage());
                log.onEvent(new BenchEvent.RunFail(passe.nom(), i + 1, runs, ms / 1000.0, e.getMessage()));
            }
        }

        boolean divergence = detectDivergence(toks);
        if (divergence) log.onEvent(new BenchEvent.Divergence(passe.nom(), tokResume(toks)));

        log.onEvent(new BenchEvent.PasseComplete(passe, model, think, elapsed, toks, tpsList, texts));

        return new BenchPasseResult(model, think, true,
                elapsed.stream().mapToLong(Long::longValue).toArray(),
                toks.stream().mapToInt(Integer::intValue).toArray(),
                tpsList.stream().mapToDouble(Double::doubleValue).toArray());
    }

    private boolean detectDivergence(List<Integer> toks) {
        if (toks.size() < 2) return false;
        double mean = toks.stream().mapToInt(Integer::intValue).average().orElse(0);
        if (mean == 0) return false;
        return toks.stream().anyMatch(t -> Math.abs(t - mean) / mean > DIVERGENCE_THRESHOLD);
    }

    private String tokResume(List<Integer> toks) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < toks.size(); i++) {
            if (i > 0) sb.append(" / ");
            sb.append(toks.get(i));
        }
        return sb.toString();
    }

    private List<ModelEntry> filtrer(List<ModelEntry> entries) {
        List<ModelEntry> result = new ArrayList<>(entries.stream()
                .filter(e -> maxModelSizeBytes <= 0 || e.sizeBytes() <= maxModelSizeBytes)
                .toList());
        if (maxModels > 0 && result.size() > maxModels) result = result.subList(0, maxModels);
        return result;
    }
}
