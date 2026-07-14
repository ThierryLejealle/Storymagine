package storymagine.commun.infra;

import storymagine.commun.coeur.ports.LogPort;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Console implementation of LogPort — columnar, timestamped, with running LLM token totals.
 */
public class ConsoleLogAdapter implements LogPort {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final int    NAME_W  = 30;
    private static final String SEP     = "  ";
    private static final String ANSI_RESET   = "[0m";
    private static final String ANSI_BOLD    = "[1m";
    private static final String ANSI_RED     = "[31m";
    private static final String ANSI_GREEN   = "[32m";
    private static final String ANSI_YELLOW  = "[33m";
    private static final String ANSI_BLUE    = "[34m";
    private static final String ANSI_CYAN    = "[36m";
    private static final String ANSI_ORANGE  = "[38;5;208m";
    private static final String ANSI_MAGENTA = "[35m";

    private final AtomicInteger llmCalls    = new AtomicInteger(0);
    private final AtomicLong    sumTokIn    = new AtomicLong(0);
    private final AtomicLong    sumTokOut   = new AtomicLong(0);
    private final AtomicLong    sumMs       = new AtomicLong(0);
    private volatile long       startMs     = System.currentTimeMillis();

    // ── LogPort ──────────────────────────────────────────────────────────────

    @Override
    public void phaseHeader(String label, String detail) {
        if (detail == null || detail.isBlank()) {
            printf("%n[%s] " + ANSI_BOLD + ANSI_BLUE + "[%s]" + ANSI_RESET + "%n", ts(), label);
        } else {
            printf("%n[%s] " + ANSI_BOLD + ANSI_BLUE + "[%s] %s" + ANSI_RESET + "%n", ts(), label, detail);
        }
    }

    @Override
    public void step(String name, long ms, String note) {
        String noteStr = (note != null && !note.isBlank()) ? SEP + note : "";
        printf("[%s]   " + ANSI_CYAN + "%-" + NAME_W + "s" + ANSI_RESET + "  %5ds%s%n",
               ts(), name, ms / 1000, noteStr);
    }

    @Override
    public void critic(String name, double score, long ms, List<String> problems) {
        String tag   = problems.isEmpty() ? SEP + "OK" : "";
        String color = scoreColor(score);
        printf("[%s]   " + color + "%-" + NAME_W + "s  %5.2f  %5ds%s" + ANSI_RESET + "%n",
               ts(), name, score, ms / 1000, tag);
        if (score >= 10.0) {
            for (String p : problems) {
                printf("[%s]     " + ANSI_RED + "! %s" + ANSI_RESET + "%n", ts(), p);
            }
        }
    }

    @Override
    public void scoresSummary(double avg, double avgThreshold, double minScore, double eliminationThreshold,
                               boolean passed, boolean forcedRetry, String retryHint) {
        String hint   = (retryHint != null && !retryHint.isBlank()) ? " " + retryHint : "";
        String status = statusLabel(passed, forcedRetry);
        if (avg <= 0 || Double.isNaN(avg)) {
            printf("[%s]   -> %s%s%n", ts(), status, hint);
        } else {
            printf("[%s]   -> moy %.2f (seuil %.1f)  min %.2f (elim %.1f)  %s%s%n",
                   ts(), avg, avgThreshold, minScore, eliminationThreshold, status, hint);
        }
    }

    /** PASS (green, real pass) / REFINE (orange, retrying despite passed scores) / RETRY (red, real score failure). */
    private static String statusLabel(boolean passed, boolean forcedRetry) {
        if (passed && !forcedRetry) return ANSI_GREEN + "PASS" + ANSI_RESET;
        if (passed) return ANSI_ORANGE + "REFINE" + ANSI_RESET;
        return ANSI_RED + "RETRY" + ANSI_RESET;
    }

    @Override
    public void warn(String message) {
        printf("[%s]   " + ANSI_RED + "[WARN]" + ANSI_RESET + " %s%n", ts(), message);
    }

    @Override
    public void info(String message) {
        printf("[%s]   " + ANSI_ORANGE + "[INFO]" + ANSI_RESET + " %s%n", ts(), message);
    }

    @Override
    public void llmCall(String agentLabel, long ms, int tokIn, int tokOut, double tokPerSec, Boolean think) {
        int    n     = llmCalls.incrementAndGet();
        long   sumI  = sumTokIn.addAndGet(tokIn);
        long   sumO  = sumTokOut.addAndGet(tokOut);
        long   total = (long) tokIn + tokOut;
        sumMs.addAndGet(ms);
        String tpsStr   = tokPerSec > 0 ? String.format("  %.1f tok/s", tokPerSec) : "";
        String thinkStr = Boolean.TRUE.equals(think) ? "  [think]" : "";
        // tokOut (eval_count) est deja le total genere par Ollama, reflexion "thinking" comprise —
        // l'API ne distingue pas les tokens de reflexion des tokens de reponse finale.
        printf("[%s]   " + ANSI_MAGENTA + "[LLM #%3d]" + ANSI_RESET + "  %-28s  %5ds  %6d -> %5d tok (total %s)%s%s  [sum in:%s out:%s total:%s]%n",
               ts(), n, agentLabel, ms / 1000, tokIn, tokOut, fmtTok(total), tpsStr, thinkStr,
               fmtTok(sumI), fmtTok(sumO), fmtTok(sumI + sumO));
    }

    @Override
    public void chapterPlan(String chapterTitle, String planText) {
        // console only shows a short acknowledgement
        printf("[%s]   [plan sauvegarde] %s%n", ts(), truncate(chapterTitle, 40));
    }

    @Override
    public void planRetained(int bestAttempt, int totalAttempts, double bestScore) {
        printf("[%s]   " + ANSI_GREEN + "-> plan retenu : tentative %d/%d  moy %.2f" + ANSI_RESET + "%n",
               ts(), bestAttempt, totalAttempts, bestScore);
    }

    @Override
    public void sequenceRetained(int bestPass, int totalPasses, double bestScore) {
        printf("[%s]   " + ANSI_GREEN + "-> sequence retenue : passe %d/%d  moy %.2f" + ANSI_RESET + "%n",
               ts(), bestPass, totalPasses, bestScore);
    }

    @Override
    public void chapterRetained(int bestPass, int totalPasses, double bestScore) {
        printf("[%s]   " + ANSI_GREEN + "-> chapitre retenu : passe %d/%d  moy %.2f" + ANSI_RESET + "%n",
               ts(), bestPass, totalPasses, bestScore);
    }

    @Override
    public void sequenceText(String chapterTitle, int seqIdx, String text) {
        int words = text == null ? 0 : text.split("\\s+").length;
        printf("[%s]   [seq %d sauvegardee] %s  (%d mots)%n",
               ts(), seqIdx, truncate(chapterTitle, 40), words);
    }

    @Override
    public void sessionEnd() {
        int  calls   = llmCalls.get();
        long totIn   = sumTokIn.get();
        long totOut  = sumTokOut.get();
        long elapsed = System.currentTimeMillis() - startMs;

        final int W = 54;
        String top = "╔" + "═".repeat(W) + "╗";
        String bot = "╚" + "═".repeat(W) + "╝";
        String fmt = "║  %-24s : %-" + (W - 28) + "s  ║%n";

        System.out.println();
        System.out.println(ANSI_BOLD + ANSI_BLUE + top + ANSI_RESET);
        printf(ANSI_BOLD + ANSI_BLUE + fmt + ANSI_RESET, "Appels LLM", calls);
        if (totIn > 0) {
            printf(ANSI_BOLD + ANSI_BLUE + fmt + ANSI_RESET, "Tokens in",  fmtTokLong(totIn));
            printf(ANSI_BOLD + ANSI_BLUE + fmt + ANSI_RESET, "Tokens out", fmtTokLong(totOut) + " (dont reflexion)");
            printf(ANSI_BOLD + ANSI_BLUE + fmt + ANSI_RESET, "Tokens total", fmtTokLong(totIn + totOut));
        }
        printf(ANSI_BOLD + ANSI_BLUE + fmt + ANSI_RESET, "Duree totale", fmtDuration(elapsed));
        System.out.println(ANSI_BOLD + ANSI_BLUE + bot + ANSI_RESET);
        System.out.println();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static void printf(String fmt, Object... args) {
        System.out.printf(Locale.ROOT, fmt, args);
    }

    private static String ts() {
        return LocalTime.now().format(TIME);
    }

    private static String scoreColor(double score) {
        if (score >= 10.0) return ANSI_BLUE;
        if (score >= 8.0)  return ANSI_GREEN;
        if (score >= 5.0)  return ANSI_YELLOW;
        if (score >= 3.0)  return ANSI_ORANGE;
        return ANSI_RED;
    }

    private static String fmtTok(long tok) {
        if (tok >= 1_000_000) return String.format(Locale.ROOT, "%.1fM", tok / 1_000_000.0);
        if (tok >= 1_000)     return String.format(Locale.ROOT, "%.1fk", tok / 1_000.0);
        return String.valueOf(tok);
    }

    private static String fmtTokLong(long tok) {
        if (tok >= 1_000_000) return String.format("%,d (~%.1fM)", tok, tok / 1_000_000.0);
        if (tok >= 1_000)     return String.format("%,d (~%.1fk)", tok, tok / 1_000.0);
        return String.valueOf(tok);
    }

    private static String fmtDuration(long ms) {
        long s = ms / 1000;
        if (s < 60)   return s + "s";
        if (s < 3600) return (s / 60) + "m " + String.format("%02d", s % 60) + "s";
        return (s / 3600) + "h " + String.format("%02d", (s % 3600) / 60) + "m";
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "~";
    }
}
