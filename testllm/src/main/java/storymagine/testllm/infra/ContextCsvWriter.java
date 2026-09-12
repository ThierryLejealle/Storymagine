package storymagine.testllm.infra;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Locale;

/**
 * Exporte les resultats de TestContextCli (balayage num_ctx sur un modele) dans un CSV cumulatif.
 * Pas de mesure d'utilisation CPU/GPU systeme (nvidia-smi/typeperf) : non scopee au processus
 * Ollama, elle capte n'importe quelle autre activite de la machine — bruit, pas signal, pour un
 * test qui vise a isoler l'effet de num_ctx. Seule la repartition ollama_processor (issue de
 * /api/ps, propre a ce modele) est fiable ici.
 */
public class ContextCsvWriter {

    private static final String SEP    = ";";
    private static final String HEADER =
            "bench_date;bench_heure;modele;gpu_mode;runs;ctx_demande;ctx_verifie_ps;statut;" +
            "tok_s_moy;ollama_processor";

    /** Une ligne de resultat : une taille de contexte testee. */
    public record Ligne(int ctxDemande, int ctxVerifiePs, String statut, double tokSMoy,
                        String ollamaProcessor) {}

    public void appendResults(Path csvFile, String date, String heure, String modele, String gpuMode,
                              int runs, List<Ligne> lignes) throws IOException {
        boolean writeHeader = !Files.exists(csvFile) || Files.size(csvFile) == 0;

        StringBuilder sb = new StringBuilder();
        if (writeHeader) sb.append(HEADER).append('\n');

        for (Ligne l : lignes) {
            sb.append(date).append(SEP).append(heure).append(SEP)
              .append(escape(modele)).append(SEP)
              .append(gpuMode).append(SEP)
              .append(runs).append(SEP)
              .append(l.ctxDemande()).append(SEP)
              .append(l.ctxVerifiePs() > 0 ? String.valueOf(l.ctxVerifiePs()) : "").append(SEP)
              .append(l.statut()).append(SEP)
              .append(l.tokSMoy() > 0 ? fmt2(l.tokSMoy()) : "").append(SEP)
              .append(l.ollamaProcessor()).append('\n');
        }

        Files.writeString(csvFile, sb.toString(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private static String fmt2(double v) { return String.format(Locale.US, "%.2f", v); }

    private static String escape(String v) {
        if (v.contains(SEP) || v.contains("\"") || v.contains("\n"))
            return "\"" + v.replace("\"", "\"\"") + "\"";
        return v;
    }
}
