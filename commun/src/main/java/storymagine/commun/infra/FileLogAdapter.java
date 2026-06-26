package storymagine.commun.infra;

import storymagine.commun.coeur.ports.LogPort;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * File implementation of LogPort.
 * Writes master-log.txt, chapters/{slug}/plan.md, chapters/{slug}/seq_N.md
 * under a timestamped run directory inside the configured output base directory.
 *
 * Lazy init: the run directory is created on the first write after setOutputDir() is called.
 * If setOutputDir() is never called, all writes are silently dropped.
 */
public class FileLogAdapter implements LogPort {

    private static final DateTimeFormatter TIME     = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter RUN_TS   = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH'h'mm");
    private static final DateTimeFormatter TRACE_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int               NAME_W   = 30;
    private static final int               MS_W     = 8;
    private static final String            SEP      = "  ";

    private volatile Path   outputDir = null;
    private volatile Path   runDir    = null;

    private final AtomicInteger llmCalls     = new AtomicInteger(0);
    private final AtomicLong    sumTokIn     = new AtomicLong(0);
    private final AtomicLong    sumTokOut    = new AtomicLong(0);
    private volatile long       startMs      = 0;

    private final AtomicInteger                traceCounter = new AtomicInteger(0);
    private final Map<String, TraceState>      openTraces   = new ConcurrentHashMap<>();

    private static final class TraceState {
        final String startTime;
        final String agentName;
        final int    localNum;
        final String systemPrompt;
        final String userPrompt;

        TraceState(String startTime, String agentName, int localNum,
                   String systemPrompt, String userPrompt) {
            this.startTime    = startTime;
            this.agentName    = agentName;
            this.localNum     = localNum;
            this.systemPrompt = systemPrompt;
            this.userPrompt   = userPrompt;
        }
    }

    /** Call after the scenario is selected, before generation starts. */
    public void setOutputDir(Path dir) {
        this.outputDir = dir;
        this.startMs   = System.currentTimeMillis();
    }

    /** Returns the run directory path (null if setOutputDir was never called or first write hasn't happened yet). */
    public Path runDir() {
        return runDir;
    }

    // ── LogPort ──────────────────────────────────────────────────────────────

    @Override
    public void phaseHeader(String label, String detail) {
        if (detail == null || detail.isBlank()) {
            appendMaster(String.format("%n[%s] [%s]%n", ts(), label));
        } else {
            appendMaster(String.format("%n[%s] [%s] %s%n", ts(), label, detail));
        }
    }

    @Override
    public void step(String name, long ms, String note) {
        String noteStr = (note != null && !note.isBlank()) ? SEP + note : "";
        appendMaster(String.format("[%s]   %-" + NAME_W + "s  %," + MS_W + "dms%s%n",
                                   ts(), name, ms, noteStr));
    }

    @Override
    public void critic(String name, double score, long ms, List<String> problems) {
        String tag = problems.isEmpty() ? SEP + "OK" : "";
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.ROOT,
                "[%s]   %-" + NAME_W + "s  %5.2f  %," + MS_W + "dms%s%n",
                ts(), name, score, ms, tag));
        for (String p : problems) {
            sb.append(String.format("[%s]     ! %s%n", ts(), p));
        }
        appendMaster(sb.toString());
    }

    @Override
    public void scoresSummary(double avg, boolean passed, String retryHint) {
        String hint = (retryHint != null && !retryHint.isBlank()) ? " " + retryHint : "";
        if (avg <= 0 || Double.isNaN(avg)) {
            appendMaster(String.format("[%s]   -> %s%s%n", ts(), passed ? "PASS" : "RETRY", hint));
        } else {
            appendMaster(String.format(Locale.ROOT,
                    "[%s]   -> moy %.2f  %s%s%n", ts(), avg, passed ? "PASS" : "RETRY", hint));
        }
    }

    @Override
    public void llmCall(String agentLabel, long ms, int tokIn, int tokOut, double tokPerSec) {
        int  n    = llmCalls.incrementAndGet();
        long sumI = sumTokIn.addAndGet(tokIn);
        long sumO = sumTokOut.addAndGet(tokOut);
        String tpsStr = tokPerSec > 0
                ? String.format(Locale.ROOT, "  %.1f tok/s", tokPerSec) : "";
        appendMaster(String.format(Locale.ROOT,
                "[%s]   [LLM #%3d]  %-28s  %,8dms  %6d -> %5d tok%s  [sum in:%s out:%s]%n",
                ts(), n, agentLabel, ms, tokIn, tokOut, tpsStr, fmtTok(sumI), fmtTok(sumO)));
    }

    @Override
    public void chapterPlan(String chapterTitle, String planText) {
        writeChapterFile(chapterTitle, "plan.md", planText);
    }

    @Override
    public void planRetained(int bestAttempt, int totalAttempts, double bestScore) {
        appendMaster(String.format(Locale.ROOT,
                "[%s]   -> plan retenu : tentative %d/%d  moy %.2f%n",
                ts(), bestAttempt, totalAttempts, bestScore));
    }

    @Override
    public void sequenceRetained(int bestPass, int totalPasses, double bestScore) {
        appendMaster(String.format(Locale.ROOT,
                "[%s]   -> sequence retenue : passe %d/%d  moy %.2f%n",
                ts(), bestPass, totalPasses, bestScore));
    }

    @Override
    public void chapterRetained(int bestPass, int totalPasses, double bestScore) {
        appendMaster(String.format(Locale.ROOT,
                "[%s]   -> chapitre retenu : passe %d/%d  moy %.2f%n",
                ts(), bestPass, totalPasses, bestScore));
    }

    @Override
    public void sequenceText(String chapterTitle, int seqIdx, String text) {
        writeChapterFile(chapterTitle, "seq_" + seqIdx + ".md", text);
    }

    @Override
    public void sessionEnd() {
        int  calls   = llmCalls.get();
        long totIn   = sumTokIn.get();
        long totOut  = sumTokOut.get();
        long elapsed = startMs > 0 ? System.currentTimeMillis() - startMs : 0;

        final int W = 54;
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("╔").append("═".repeat(W)).append("╗\n");
        sb.append(row(W, "Appels LLM",   String.valueOf(calls)));
        if (totIn > 0) {
            sb.append(row(W, "Tokens in",  fmtTokLong(totIn)));
            sb.append(row(W, "Tokens out", fmtTokLong(totOut)));
        }
        sb.append(row(W, "Duree totale", fmtDuration(elapsed)));
        sb.append("╚").append("═".repeat(W)).append("╝\n\n");
        appendMaster(sb.toString());
    }

    // ── LLM call trace ────────────────────────────────────────────────────────

    @Override
    public String llmCallOpen(String agentName, int localNum,
                               String systemPrompt, String userPrompt) {
        int    num   = traceCounter.incrementAndGet();
        String stem  = String.format("%03d_%s_%d", num, agentName, localNum);
        String start = LocalDateTime.now().format(TRACE_TS);
        TraceState state = new TraceState(start, agentName, localNum, systemPrompt, userPrompt);
        openTraces.put(stem, state);
        writeTraceFile(stem, state, null, -1);
        return stem;
    }

    @Override
    public void llmCallClose(String handle, String response, long elapsedMs,
                              int tokIn, int tokOut) {
        if (handle == null || handle.isEmpty()) return;
        TraceState state = openTraces.remove(handle);
        if (state == null) return;
        writeTraceFile(handle, state, response, elapsedMs);
    }

    private void writeTraceFile(String stem, TraceState state, String response, long elapsedMs) {
        boolean done = response != null && elapsedMs >= 0;

        StringBuilder header = new StringBuilder();
        header.append("## EN-TÊTE\n");
        header.append("- Démarré  : ").append(state.startTime).append("\n");
        header.append("- Statut   : ").append(done ? "✅ OK" : "⏳ En cours…").append("\n");
        header.append("- Sys      : ~").append(state.systemPrompt.length() / 4).append(" tok\n");
        header.append("- Usr      : ~").append(state.userPrompt.length()    / 4).append(" tok\n");
        if (done) {
            header.append("- Réponse  : ~").append(response.length() / 4).append(" tok\n");
            header.append("- Durée    : ").append(String.format("%.1f", elapsedMs / 1000.0)).append("s\n");
        } else {
            header.append("- Réponse  : —\n");
            header.append("- Durée    : —\n");
        }

        String responseSection = done ? response : "⏳ En attente…";
        String content = "# " + state.agentName + " — appel " + state.localNum + "\n\n"
                + header + "\n"
                + "---\n\n## PROMPT SYSTÈME\n\n" + state.systemPrompt
                + "\n\n---\n\n## PROMPT UTILISATEUR\n\n" + state.userPrompt
                + "\n\n---\n\n## RÉPONSE\n\n" + responseSection;

        Path dir = runDir != null ? runDir : initRunDir();
        if (dir == null) return;
        try {
            Path callsDir = dir.resolve("llm_calls");
            Files.createDirectories(callsDir);
            Files.writeString(callsDir.resolve(stem + ".md"), content);
        } catch (IOException ignored) {}
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void appendMaster(String text) {
        Path master = masterLogPath();
        if (master == null) return;
        try {
            Files.writeString(master, text,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {}
    }

    private void writeChapterFile(String chapterTitle, String filename, String content) {
        Path dir = runDir();
        if (dir == null) dir = initRunDir();
        if (dir == null) return;
        Path chapterDir = dir.resolve("chapters").resolve(slugify(chapterTitle));
        try {
            Files.createDirectories(chapterDir);
            Files.writeString(chapterDir.resolve(filename), content == null ? "" : content);
        } catch (IOException ignored) {}
    }

    private Path masterLogPath() {
        if (runDir != null) return runDir.resolve("master-log.txt");
        Path dir = initRunDir();
        return dir != null ? dir.resolve("master-log.txt") : null;
    }

    private synchronized Path initRunDir() {
        if (runDir != null) return runDir;
        if (outputDir == null) return null;
        String ts = LocalDateTime.now().format(RUN_TS);
        Path dir = outputDir.resolve(ts);
        try {
            Files.createDirectories(dir);
            runDir = dir;
            return dir;
        } catch (IOException ignored) {
            return null;
        }
    }

    private static String row(int W, String label, String value) {
        return String.format("║  %-24s : %-" + (W - 28) + "s  ║%n", label, value);
    }

    private static String ts() {
        return LocalTime.now().format(TIME);
    }

    private static String slugify(String title) {
        if (title == null || title.isBlank()) return "unknown";
        return title.toLowerCase(Locale.ROOT)
                    .replaceAll("[^a-z0-9]+", "_")
                    .replaceAll("^_|_$", "");
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
}
