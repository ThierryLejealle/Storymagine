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

    private final AtomicInteger llmCalls    = new AtomicInteger(0);
    private final AtomicLong    sumTokIn    = new AtomicLong(0);
    private final AtomicLong    sumTokOut   = new AtomicLong(0);
    private final AtomicLong    sumMs       = new AtomicLong(0);
    private volatile long       startMs     = System.currentTimeMillis();

    // ── LogPort ──────────────────────────────────────────────────────────────

    @Override
    public void phaseHeader(String label, String detail) {
        if (detail == null || detail.isBlank()) {
            printf("%n[%s] [%s]%n", ts(), label);
        } else {
            printf("%n[%s] [%s] %s%n", ts(), label, detail);
        }
    }

    @Override
    public void step(String name, long ms, String note) {
        String noteStr = (note != null && !note.isBlank()) ? SEP + note : "";
        printf("[%s]   %-" + NAME_W + "s  %5ds%s%n", ts(), name, ms / 1000, noteStr);
    }

    @Override
    public void critic(String name, double score, long ms, List<String> problems) {
        String tag = problems.isEmpty() ? SEP + "OK" : "";
        printf("[%s]   %-" + NAME_W + "s  %5.2f  %5ds%s%n",
               ts(), name, score, ms / 1000, tag);
        if (score >= 10.0) {
            for (String p : problems) {
                printf("[%s]     ! %s%n", ts(), p);
            }
        }
    }

    @Override
    public void scoresSummary(double avg, boolean passed, String retryHint) {
        String hint = (retryHint != null && !retryHint.isBlank()) ? " " + retryHint : "";
        if (avg <= 0 || Double.isNaN(avg)) {
            printf("[%s]   -> %s%s%n", ts(), passed ? "PASS" : "RETRY", hint);
        } else {
            printf("[%s]   -> moy %.2f  %s%s%n", ts(), avg, passed ? "PASS" : "RETRY", hint);
        }
    }

    @Override
    public void warn(String message) {
        printf("[%s]   [WARN] %s%n", ts(), message);
    }

    @Override
    public void llmCall(String agentLabel, long ms, int tokIn, int tokOut, double tokPerSec) {
        int    n    = llmCalls.incrementAndGet();
        long   sumI = sumTokIn.addAndGet(tokIn);
        long   sumO = sumTokOut.addAndGet(tokOut);
        sumMs.addAndGet(ms);
        String tpsStr = tokPerSec > 0 ? String.format("  %.1f tok/s", tokPerSec) : "";
        printf("[%s]   [LLM #%3d]  %-28s  %5ds  %6d -> %5d tok%s  [sum in:%s out:%s]%n",
               ts(), n, agentLabel, ms / 1000, tokIn, tokOut, tpsStr, fmtTok(sumI), fmtTok(sumO));
    }

    @Override
    public void chapterPlan(String chapterTitle, String planText) {
        // console only shows a short acknowledgement
        printf("[%s]   [plan sauvegarde] %s%n", ts(), truncate(chapterTitle, 40));
    }

    @Override
    public void planRetained(int bestAttempt, int totalAttempts, double bestScore) {
        printf("[%s]   -> plan retenu : tentative %d/%d  moy %.2f%n",
               ts(), bestAttempt, totalAttempts, bestScore);
    }

    @Override
    public void sequenceRetained(int bestPass, int totalPasses, double bestScore) {
        printf("[%s]   -> sequence retenue : passe %d/%d  moy %.2f%n",
               ts(), bestPass, totalPasses, bestScore);
    }

    @Override
    public void chapterRetained(int bestPass, int totalPasses, double bestScore) {
        printf("[%s]   -> chapitre retenu : passe %d/%d  moy %.2f%n",
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
        System.out.println(top);
        printf(fmt, "Appels LLM", calls);
        if (totIn > 0) {
            printf(fmt, "Tokens in",  fmtTokLong(totIn));
            printf(fmt, "Tokens out", fmtTokLong(totOut));
        }
        printf(fmt, "Duree totale", fmtDuration(elapsed));
        System.out.println(bot);
        System.out.println();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static void printf(String fmt, Object... args) {
        System.out.printf(Locale.ROOT, fmt, args);
    }

    private static String ts() {
        return LocalTime.now().format(TIME);
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
