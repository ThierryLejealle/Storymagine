package storymagine.testllm.infra;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Locale;

/**
 * Exporte les resultats de TestMtpCli dans un fichier CSV cumulatif — pendant symetrique de
 * BenchCsvWriter pour le diagnostic MTP (colonnes differentes : pas de metier bench standard).
 */
public class MtpCsvWriter {

    private static final String SEP    = ";";
    private static final String HEADER = "bench_date;bench_heure;modele;scenario;think;draft_num_predict;runs;tokens_moy;tok_s_moy";

    /** Une ligne de resultat : valeur de draft_num_predict testee ("(aucun)" ou un nombre) et moyennes sur "runs" appels. */
    public record Ligne(String draftNumPredict, double avgTokens, double avgTokPerSec) {}

    public void appendResults(Path csvFile, String date, String heure, String modele, String scenario,
                              boolean think, int runs, List<Ligne> lignes) throws IOException {
        boolean writeHeader = !Files.exists(csvFile) || Files.size(csvFile) == 0;

        StringBuilder sb = new StringBuilder();
        if (writeHeader) sb.append(HEADER).append('\n');

        for (Ligne l : lignes) {
            sb.append(date).append(SEP).append(heure).append(SEP)
              .append(escape(modele)).append(SEP)
              .append(scenario).append(SEP)
              .append(think ? "O" : "N").append(SEP)
              .append(l.draftNumPredict()).append(SEP)
              .append(runs).append(SEP)
              .append(fmt1(l.avgTokens())).append(SEP)
              .append(fmt2(l.avgTokPerSec())).append('\n');
        }

        Files.writeString(csvFile, sb.toString(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private static String fmt1(double v) { return String.format(Locale.US, "%.1f", v); }
    private static String fmt2(double v) { return String.format(Locale.US, "%.2f", v); }

    private static String escape(String v) {
        if (v.contains(SEP) || v.contains("\"") || v.contains("\n"))
            return "\"" + v.replace("\"", "\"\"") + "\"";
        return v;
    }
}
