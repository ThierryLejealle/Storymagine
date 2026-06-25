package storymagine.testllm.infra;

import storymagine.commun.infra.ModelHardwareDisplay;
import storymagine.commun.infra.NvidiaSmiSnapshot;
import storymagine.commun.infra.OllamaPsInfo;
import storymagine.testllm.coeur.domaine.BenchmarkRunner;
import storymagine.testllm.coeur.domaine.BenchmarkRunner.BenchEvent;
import storymagine.testllm.coeur.domaine.BenchPasseResult;
import storymagine.testllm.coeur.domaine.HardwareUsage;
import storymagine.testllm.coeur.domaine.PasseBench;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * RunLogger qui imprime la progression en console et ecrit les logs detailles (.md) par passe.
 * Capture aussi l'etat GPU (nvidia-smi + ollama /api/ps) apres chaque probe.
 */
public class ConsoleRunLogger implements BenchmarkRunner.RunLogger {

    private final BenchTextFormatter       formatter;
    private final BenchLogWriter           logWriter;
    private final Path                     runDir;
    private final int                      runs;
    private final StringBuilder            resume;
    private final String                   gpuMode;
    private final String                   baseUrl;

    private String                         currentModel  = null;
    private final Map<String, GpuState>    gpuStateByModel = new LinkedHashMap<>();

    public ConsoleRunLogger(BenchTextFormatter formatter, BenchLogWriter logWriter,
                            Path runDir, int runs, StringBuilder resume,
                            String gpuMode, String baseUrl) {
        this.formatter = formatter;
        this.logWriter = logWriter;
        this.runDir    = runDir;
        this.runs      = runs;
        this.resume    = resume;
        this.gpuMode   = gpuMode;
        this.baseUrl   = baseUrl;
    }

    public Map<String, GpuState> getGpuStates() {
        return Collections.unmodifiableMap(gpuStateByModel);
    }

    @Override
    public void onEvent(BenchEvent event) {
        switch (event) {
            case BenchEvent.ModelStart e -> {
                currentModel = e.model();
                System.out.printf("%n[bench] +- Modele : %s%n", e.model());
            }

            case BenchEvent.ProbeOk e -> {
                System.out.printf("[bench] |  probe -> OK (%.0fs)  ctx:%dk (max:%dk)%n",
                        e.elapsedSec(), e.ctx() / 1024, e.maxCtx() / 1024);
                String p = e.info().modelParamSize(), q = e.info().modelQuantization(), f = e.info().modelFamily();
                if (!p.isEmpty() || !q.isEmpty() || !f.isEmpty()) {
                    System.out.printf("[bench] |  params : %-8s  quant : %-12s  famille : %s%n",
                            p.isEmpty() ? "-" : p, q.isEmpty() ? "-" : q, f.isEmpty() ? "-" : f);
                }
                System.out.printf("[bench] |  thinking : %s%n", e.info().supportsThinking() ? "oui" : "non");
                if (e.isLarge()) {
                    System.out.printf("[bench] |  *** GROS MODELE (%.1f Go) — timeout etendu ***%n",
                            e.sizeBytes() / 1_073_741_824.0);
                }

                // Capture GPU state
                List<NvidiaSmiSnapshot.GpuStat> gpuStats = NvidiaSmiSnapshot.capture();
                Optional<OllamaPsInfo>          ps       = OllamaPsInfo.query(baseUrl, currentModel);
                GpuState state = new GpuState(gpuMode, ps.orElse(null), gpuStats);
                if (currentModel != null) gpuStateByModel.put(currentModel, state);

                ModelHardwareDisplay.print("[bench] |  ", ps.orElse(null), gpuStats);
            }

            case BenchEvent.ProbeFail e ->
                System.out.printf("[bench] |  probe -> FAIL (%.0fs) : %s%n", e.elapsedSec(), e.message());

            case BenchEvent.RunOk e ->
                System.out.printf("[bench] |  %-6s  run %d/%d -> OK  %5d car | %5.1f t/s | %4.0fs%n",
                        e.passe(), e.run(), e.total(), e.chars(), e.tps(), e.elapsedSec());

            case BenchEvent.RunFail e ->
                System.out.printf("[bench] |  %-6s  run %d/%d -> FAIL (%4.0fs) : %s%n",
                        e.passe(), e.run(), e.total(), e.elapsedSec(), e.message());

            case BenchEvent.Divergence e ->
                System.out.printf("[bench] |  %-6s  !! DIVERGENCE tok : %s%n", e.passe(), e.tokensResume());

            case BenchEvent.PasseComplete e -> {
                if (runDir != null) {
                    GpuState gpuState = gpuStateByModel.getOrDefault(e.model(), GpuState.ABSENT);
                    logWriter.write(runDir, e.passe(), e.model(), e.think(),
                            e.elapsed(), e.toks(), e.tps(), e.texts(), gpuState);
                }
            }

            case BenchEvent.IterationSummary e -> {
                System.out.printf("[bench] +- resume %s [think:%s]%n", e.model(), e.think() ? "O" : "N");
                List<BenchPasseResult> results = e.results();
                List<PasseBench>       passes  = e.passes();
                for (int p = 0; p < results.size(); p++) {
                    BenchPasseResult r  = results.get(p);
                    String           pn = p < passes.size() ? passes.get(p).nom() : String.valueOf(p);
                    if (!r.probeOk() || r.successCount() == 0) {
                        System.out.printf("[bench] |  %-6s : FAIL%n", pn);
                    } else {
                        System.out.printf("[bench] |  %-6s : %4d tok moy | %5.1f t/s | %4.0fs moy%n",
                                pn, r.avgTok(), r.avgTps(), r.avgMs() / 1000.0);
                    }
                }
            }

            case BenchEvent.PasseDone e -> {
                String formatted = formatter.formatPasse(e.titre(), e.resultats(), runs);
                System.out.println(formatted);
                resume.append(formatted).append(System.lineSeparator());
            }

            case BenchEvent.ModelUnload e -> {
                HardwareUsage hw = e.hardware();
                if (hw != null && hw != HardwareUsage.ABSENT) {
                    System.out.printf(
                        "[bench] |  hw avg  : GPU0=%3d%%  GPU1=%3d%%  CPU=%3d%%%n",
                        hw.gpu0Pct() >= 0 ? hw.gpu0Pct() : 0,
                        hw.gpu1Pct() >= 0 ? hw.gpu1Pct() : 0,
                        hw.cpuPct()  >= 0 ? hw.cpuPct()  : 0);
                }
                System.out.printf("[bench] +- dechargement %s%n", e.model());
            }
        }
    }
}
