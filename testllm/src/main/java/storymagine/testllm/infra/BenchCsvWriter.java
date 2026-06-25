package storymagine.testllm.infra;

import storymagine.commun.infra.NvidiaSmiSnapshot;
import storymagine.testllm.coeur.domaine.BenchModeleResult;
import storymagine.testllm.coeur.domaine.BenchPasseResult;
import storymagine.testllm.coeur.domaine.BenchRun;
import storymagine.testllm.coeur.domaine.HardwareUsage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Exporte les resultats de benchmark dans un fichier CSV cumulatif.
 */
public class BenchCsvWriter {

    private static final String SEP    = ";";
    private static final String HEADER =
            "bench_date;bench_heure;modele;famille;params;quant;think;type_test;" +
            "ram_mo;tps_moy_s;tok_moy;tok_min;tok_max;tok_s_moy;ctx_utilise;ctx_max;err;divergence;" +
            "gpu_mode;ollama_size_go;ollama_processor;gpu0_used_mo;gpu0_total_mo;gpu1_used_mo;gpu1_total_mo;" +
            "gpu0_util_pct;gpu1_util_pct;cpu_util_pct";

    public void appendResults(Path csvFile, String date, String heure,
                              BenchRun run, Map<String, GpuState> gpuStates) throws IOException {
        boolean writeHeader = !Files.exists(csvFile) || Files.size(csvFile) == 0;

        List<BenchModeleResult>              modeles  = run.modeles();
        Map<Integer, List<BenchPasseResult>> parPasse = run.parPasse();
        List<String> passNoms = run.passes().stream().map(p -> p.nom()).toList();

        StringBuilder sb = new StringBuilder();
        if (writeHeader) sb.append(HEADER).append('\n');

        for (int i = 0; i < modeles.size(); i++) {
            BenchModeleResult s        = modeles.get(i);
            GpuState          gpuState = gpuStates.getOrDefault(s.modele(), GpuState.ABSENT);

            for (int p = 0; p < run.passes().size(); p++) {
                List<BenchPasseResult> passResults = parPasse.get(p);
                if (passResults == null || i >= passResults.size()) continue;
                BenchPasseResult r   = passResults.get(i);
                String           nom = p < passNoms.size() ? passNoms.get(p) : String.valueOf(p);

                String think = s.think() ? "O" : "N";
                String ram   = s.avgMemMb() >= 0 ? String.valueOf(s.avgMemMb()) : "";
                String ctxU  = s.ctx()    > 0 ? (s.ctx()    / 1024) + "k" : "";
                String ctxM  = s.maxCtx() > 0 ? (s.maxCtx() / 1024) + "k" : "";

                String tpsMoy, tokMoy, tokMin, tokMax, tokSMoy, err, div;
                if (!r.probeOk()) {
                    tpsMoy = ""; tokMoy = ""; tokMin = ""; tokMax = ""; tokSMoy = "";
                    err = "probe_fail"; div = "N";
                } else if (r.successCount() == 0) {
                    tpsMoy = ""; tokMoy = ""; tokMin = ""; tokMax = ""; tokSMoy = "";
                    err = "no_success"; div = "N";
                } else {
                    tpsMoy  = fmt1(r.avgMs() / 1000.0);
                    tokMoy  = String.valueOf(r.avgTok());
                    tokMin  = String.valueOf(r.minTok());
                    tokMax  = String.valueOf(r.maxTok());
                    tokSMoy = fmt1(r.avgTps());
                    err     = "";
                    div     = r.divergence() ? "O" : "N";
                }

                // GPU columns
                String gpuMode    = gpuState.gpuMode();
                String ollamaSize = gpuState.ps() != null ? gpuState.ps().sizeLabel()      : "";
                String ollamaProc = gpuState.ps() != null ? gpuState.ps().processorLabel() : "";
                String gpu0used = "", gpu0total = "", gpu1used = "", gpu1total = "";
                for (NvidiaSmiSnapshot.GpuStat g : gpuState.gpuStats()) {
                    if (g.index() == 0) { gpu0used = String.valueOf(g.usedMb()); gpu0total = String.valueOf(g.totalMb()); }
                    if (g.index() == 1) { gpu1used = String.valueOf(g.usedMb()); gpu1total = String.valueOf(g.totalMb()); }
                }

                // Hardware utilization averages
                HardwareUsage hw     = s.avgHardware() != null ? s.avgHardware() : HardwareUsage.ABSENT;
                String gpu0util = hw.gpu0Pct() >= 0 ? String.valueOf(hw.gpu0Pct()) : "";
                String gpu1util = hw.gpu1Pct() >= 0 ? String.valueOf(hw.gpu1Pct()) : "";
                String cpuUtil  = hw.cpuPct()  >= 0 ? String.valueOf(hw.cpuPct())  : "";

                sb.append(date).append(SEP).append(heure).append(SEP)
                  .append(escape(s.modele())).append(SEP)
                  .append(s.famille()).append(SEP)
                  .append(s.paramSize()).append(SEP)
                  .append(s.quantization()).append(SEP)
                  .append(think).append(SEP)
                  .append(nom).append(SEP)
                  .append(ram).append(SEP)
                  .append(tpsMoy).append(SEP)
                  .append(tokMoy).append(SEP)
                  .append(tokMin).append(SEP)
                  .append(tokMax).append(SEP)
                  .append(tokSMoy).append(SEP)
                  .append(ctxU).append(SEP)
                  .append(ctxM).append(SEP)
                  .append(err).append(SEP)
                  .append(div).append(SEP)
                  .append(gpuMode).append(SEP)
                  .append(ollamaSize).append(SEP)
                  .append(ollamaProc).append(SEP)
                  .append(gpu0used).append(SEP)
                  .append(gpu0total).append(SEP)
                  .append(gpu1used).append(SEP)
                  .append(gpu1total).append(SEP)
                  .append(gpu0util).append(SEP)
                  .append(gpu1util).append(SEP)
                  .append(cpuUtil).append('\n');
            }
        }

        Files.writeString(csvFile, sb.toString(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private static String fmt1(double v) { return String.format(Locale.US, "%.1f", v); }

    private static String escape(String v) {
        if (v.contains(SEP) || v.contains("\"") || v.contains("\n"))
            return "\"" + v.replace("\"", "\"\"") + "\"";
        return v;
    }
}
