package storymagine.testllm.infra;

import storymagine.testllm.coeur.domaine.BenchModeleResult;
import storymagine.testllm.coeur.domaine.BenchPasseResult;
import storymagine.testllm.coeur.domaine.BenchRun;
import storymagine.testllm.coeur.domaine.HardwareUsage;
import storymagine.testllm.coeur.domaine.PasseBench;

import java.util.List;
import java.util.Map;

/**
 * Formate les résultats de benchmark en tableaux texte lisibles.
 */
public class BenchTextFormatter {

    private static final int W_THINK = 5;
    private static final int W_TOK   = 18;
    private static final int W_TEMPS = 11;
    private static final int W_TPS   = 7;
    private static final int W_MEM   = 8;
    private static final int W_CTX   = 9;
    private static final int W_ERR   = 3;

    /** Tableau par passe (une ligne par modèle). */
    public String formatPasse(String titre, List<BenchPasseResult> resultats, int runs) {
        int W = Math.max("Modèle".length(),
                resultats.stream().mapToInt(r -> r.modele().length()).max().orElse(10));

        StringBuilder sb = new StringBuilder();
        sb.append("=== ").append(titre).append(" ===").append(nl()).append(nl());
        sb.append(String.format(
                "%-" + W + "s | %-" + W_THINK + "s | %-" + W_TOK + "s | %-" + W_TEMPS
                + "s | %" + W_TPS + "s | %" + W_MEM + "s | %" + W_CTX + "s | %-" + W_ERR + "s%n",
                "Modèle", "Think", "car min/moy/max", "tps moy (s)", "t/s moy", "RAM (Mo)", "ctx/max", "Err"));
        sb.append(sep(W));

        for (BenchPasseResult r : resultats) {
            String mem   = r.avgMemMb() >= 0 ? String.valueOf(r.avgMemMb()) : "-";
            String think = r.think() ? "O" : "N";
            String err   = r.divergence() ? " ? " : "   ";
            if (!r.probeOk()) {
                sb.append(String.format(
                        "%-" + W + "s | %-" + W_THINK + "s | %-" + W_TOK + "s | %-" + W_TEMPS
                        + "s | %" + W_TPS + "s | %" + W_MEM + "s | %" + W_CTX + "s | %-" + W_ERR + "s%n",
                        r.modele(), think, "probe FAIL", "-", "-", "-", "-", "   "));
            } else if (r.successCount() == 0) {
                sb.append(String.format(
                        "%-" + W + "s | %-" + W_THINK + "s | %-" + W_TOK + "s | %-" + W_TEMPS
                        + "s | %" + W_TPS + "s | %" + W_MEM + "s | %" + W_CTX + "s | %-" + W_ERR + "s%n",
                        r.modele(), think, "0/" + runs + " OK", "-", "-", "-", "-", "   "));
            } else {
                sb.append(String.format(
                        "%-" + W + "s | %-" + W_THINK + "s | %4d / %4d / %4d | %11.1f | %7.1f | %-"
                        + W_MEM + "s | %" + W_CTX + "s | %-" + W_ERR + "s%n",
                        r.modele(), think,
                        r.minTok(), r.avgTok(), r.maxTok(),
                        r.avgMs() / 1000.0, r.avgTps(),
                        mem, r.ctxLabel() + "/" + r.maxCtxLabel(), err));
            }
        }
        return sb.toString();
    }

    /** Tableau de synthèse globale (une ligne par modèle, toutes passes confondues). */
    public String formatSynthese(List<BenchModeleResult> modeles) {
        if (modeles.isEmpty()) return "";

        int WM  = Math.max("Modèle".length(),  modeles.stream().mapToInt(s -> s.modele().length()).max().orElse(10));
        int WF  = Math.max("Famille".length(),  modeles.stream().mapToInt(s -> s.famille().length()).max().orElse(7));
        int WP  = Math.max("Params".length(),   modeles.stream().mapToInt(s -> s.paramSize().length()).max().orElse(6));
        int WQ  = Math.max("Quant".length(),    modeles.stream().mapToInt(s -> s.quantization().length()).max().orElse(5));
        int WC  = 9, WME = 8, WT = 14, WG = 5;

        StringBuilder sb = new StringBuilder();
        sb.append("=== SYNTHÈSE GLOBALE ===").append(nl()).append(nl());
        sb.append(String.format(
                "%-" + WM + "s | %-" + W_THINK + "s | %-" + WF + "s | %-" + WP + "s | %-" + WQ
                + "s | %" + WC + "s | %" + WME + "s | %" + WT + "s | %" + WG + "s | %" + WG + "s | %" + WG + "s | %-" + W_ERR + "s%n",
                "Modèle", "Think", "Famille", "Params", "Quant", "ctx/max", "RAM (Mo)", "t/s moy global",
                "GPU0%", "GPU1%", "CPU%", "Err"));
        sb.append(sepMulti(WM, WF, WP, WQ, WC, WME, WT, WG));

        for (BenchModeleResult s : modeles) {
            String mem = s.avgMemMb() >= 0 ? String.valueOf(s.avgMemMb()) : "-";
            String ctx = (s.ctx()    > 0 ? s.ctx()    / 1024 + "k" : "-")
                       + "/" + (s.maxCtx() > 0 ? s.maxCtx() / 1024 + "k" : "-");
            String tps = s.globalAvgTps() > 0 ? String.format("%.1f", s.globalAvgTps()) : "-";
            HardwareUsage hw   = s.avgHardware() != null ? s.avgHardware() : HardwareUsage.ABSENT;
            String gpu0 = hw.gpu0Pct() >= 0 ? hw.gpu0Pct() + "%" : "-";
            String gpu1 = hw.gpu1Pct() >= 0 ? hw.gpu1Pct() + "%" : "-";
            String cpu  = hw.cpuPct()  >= 0 ? hw.cpuPct()  + "%" : "-";
            sb.append(String.format(
                    "%-" + WM + "s | %-" + W_THINK + "s | %-" + WF + "s | %-" + WP + "s | %-" + WQ
                    + "s | %" + WC + "s | %" + WME + "s | %" + WT + "s | %" + WG + "s | %" + WG + "s | %" + WG + "s | %-" + W_ERR + "s%n",
                    s.modele(), s.think() ? "O" : "N",
                    s.famille().isEmpty()       ? "-" : s.famille(),
                    s.paramSize().isEmpty()     ? "-" : s.paramSize(),
                    s.quantization().isEmpty()  ? "-" : s.quantization(),
                    ctx, mem, tps, gpu0, gpu1, cpu, s.divergence() ? " ? " : "   "));
        }
        return sb.toString();
    }

    /** Tableau par modèle (toutes passes pour chaque modèle). */
    public String formatParModele(BenchRun run) {
        if (run.modeles().isEmpty()) return "";

        final int W_PASS = 5;
        String colHeader = String.format(
                "%-" + W_PASS + "s | %-" + W_TOK + "s | %-" + W_TEMPS + "s | %" + W_TPS + "s | %-" + W_ERR + "s%n",
                "Passe", "car min/moy/max", "tps moy (s)", "t/s moy", "Err");
        String separator = "─".repeat(W_PASS) + "-+-" + "─".repeat(W_TOK) + "-+-"
                + "─".repeat(W_TEMPS) + "-+-" + "─".repeat(W_TPS) + "-+-"
                + "─".repeat(W_ERR) + nl();

        StringBuilder sb = new StringBuilder();
        sb.append("=== PAR MODÈLE ===").append(nl()).append(nl());

        List<BenchModeleResult>              modeles  = run.modeles();
        Map<Integer, List<BenchPasseResult>> parPasse = run.parPasse();
        List<PasseBench>                     passes   = run.passes();

        for (int i = 0; i < modeles.size(); i++) {
            BenchModeleResult s = modeles.get(i);
            sb.append("--- ").append(s.modele());
            if (s.think()) sb.append(" [think:O]");
            sb.append(" ---").append(nl());
            HardwareUsage hw = s.avgHardware() != null ? s.avgHardware() : HardwareUsage.ABSENT;
            if (hw != HardwareUsage.ABSENT) {
                sb.append(String.format("hw avg : GPU0=%3s  GPU1=%3s  CPU=%3s%n",
                        hw.gpu0Pct() >= 0 ? hw.gpu0Pct() + "%" : "-",
                        hw.gpu1Pct() >= 0 ? hw.gpu1Pct() + "%" : "-",
                        hw.cpuPct()  >= 0 ? hw.cpuPct()  + "%" : "-"));
            }
            sb.append(colHeader).append(separator);

            for (int p = 0; p < passes.size(); p++) {
                String passNom = passes.get(p).nom();
                List<BenchPasseResult> passResults = parPasse.get(p);
                if (passResults == null || i >= passResults.size()) continue;
                BenchPasseResult r = passResults.get(i);
                String err = r.divergence() ? " ? " : "   ";
                if (!r.probeOk()) {
                    sb.append(String.format("%-" + W_PASS + "s | %-" + W_TOK + "s | %-" + W_TEMPS
                            + "s | %" + W_TPS + "s | %-" + W_ERR + "s%n", passNom, "SKIP", "-", "-", "   "));
                } else if (r.successCount() == 0) {
                    sb.append(String.format("%-" + W_PASS + "s | %-" + W_TOK + "s | %-" + W_TEMPS
                            + "s | %" + W_TPS + "s | %-" + W_ERR + "s%n", passNom, "0 OK", "-", "-", "   "));
                } else {
                    sb.append(String.format("%-" + W_PASS + "s | %4d / %4d / %4d | %11.1f | %7.1f | %-" + W_ERR + "s%n",
                            passNom, r.minTok(), r.avgTok(), r.maxTok(),
                            r.avgMs() / 1000.0, r.avgTps(), err));
                }
            }
            sb.append(nl());
        }
        return sb.toString();
    }

    /** Avertissements de divergence. Retourne "" si aucune. */
    public String formatDivergences(List<BenchModeleResult> modeles) {
        List<BenchModeleResult> flagged = modeles.stream().filter(BenchModeleResult::divergence).toList();
        if (flagged.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("=== AVERTISSEMENTS DIVERGENCE ===").append(nl()).append(nl())
          .append("  Ecart tokens > 50% :").append(nl());
        for (BenchModeleResult s : flagged) {
            sb.append(String.format("  - %-40s [think:%s]%n", s.modele(), s.think() ? "O" : "N"));
        }
        return sb.toString();
    }

    private static String nl() { return System.lineSeparator(); }

    private static String sep(int W) {
        return "─".repeat(W) + "-+-" + "─".repeat(W_THINK) + "-+-" + "─".repeat(W_TOK)
             + "-+-" + "─".repeat(W_TEMPS) + "-+-" + "─".repeat(W_TPS)
             + "-+-" + "─".repeat(W_MEM) + "-+-" + "─".repeat(W_CTX)
             + "-+-" + "─".repeat(W_ERR) + nl();
    }

    private static String sepMulti(int WM, int WF, int WP, int WQ, int WC, int WME, int WT, int WG) {
        return "─".repeat(WM) + "-+-" + "─".repeat(W_THINK) + "-+-" + "─".repeat(WF) + "-+-"
             + "─".repeat(WP) + "-+-" + "─".repeat(WQ) + "-+-" + "─".repeat(WC) + "-+-"
             + "─".repeat(WME) + "-+-" + "─".repeat(WT) + "-+-"
             + "─".repeat(WG) + "-+-" + "─".repeat(WG) + "-+-" + "─".repeat(WG) + "-+-"
             + "─".repeat(W_ERR) + nl();
    }
}
