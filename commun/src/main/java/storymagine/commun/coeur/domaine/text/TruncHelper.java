package storymagine.commun.coeur.domaine.text;

import storymagine.commun.coeur.ports.LogPort;

import java.util.ArrayList;
import java.util.List;

/**
 * Truncates prompt fields to a character budget, tracking which fields got cut
 * so the caller can emit a single consolidated warning instead of one log line
 * per field. Knows nothing about the caller's domain — only about text and line
 * boundaries.
 */
public final class TruncHelper {

    private final List<String> cutLabels = new ArrayList<>();

    private TruncHelper() {
    }

    public static TruncHelper create() {
        return new TruncHelper();
    }

    /**
     * Truncates prose to maxChars, cutting at the last sentence or line boundary
     * found before the limit rather than mid-sentence. Falls back to a hard cut
     * with "…" when no boundary is found in the window.
     */
    public String text(String s, int maxChars, String label) {
        if (s == null || s.isBlank()) return "";
        if (s.length() <= maxChars) return s;
        cutLabels.add(label);
        return cutHead(s, maxChars);
    }

    /**
     * Truncates prose to maxChars, keeping the END of the text instead of the start,
     * and starting at the next sentence boundary found in the kept tail.
     */
    public String tailText(String s, int maxChars, String label) {
        if (s == null || s.isBlank()) return "";
        if (s.length() <= maxChars) return s;
        cutLabels.add(label);
        String tail = s.substring(s.length() - maxChars);
        int dot = tail.indexOf('.');
        return dot >= 0 ? tail.substring(dot + 1).trim() : tail.trim();
    }

    /**
     * Truncates a "\n"-joined list of items to maxChars, keeping only whole lines —
     * never cuts an item in half. Drops the trailing partial line(s) instead.
     */
    public String list(String s, int maxChars, String label) {
        if (s == null || s.isBlank()) return "";
        if (s.length() <= maxChars) return s;
        cutLabels.add(label);
        return cutLines(s, maxChars);
    }

    /**
     * Truncates a "\n\n"-joined list of multi-line blocks (e.g. character sheets, one
     * blank-line-separated entry per item) to maxChars, keeping only whole blocks —
     * never cuts a block in half. Drops the trailing partial block instead.
     */
    public String blockList(String s, int maxChars, String label) {
        if (s == null || s.isBlank()) return "";
        if (s.length() <= maxChars) return s;
        cutLabels.add(label);
        return cutBlocks(s, maxChars);
    }

    /** Emits one warning listing every field truncated so far, or nothing if none was. */
    public void logIfTruncated(LogPort log, String agentName) {
        if (cutLabels.isEmpty()) return;
        log.warn(agentName + ": troncature — " + String.join(", ", cutLabels));
    }

    private static String cutHead(String s, int maxChars) {
        String head = s.substring(0, maxChars);
        int best = -1;
        for (int i = head.length() - 1; i >= 0; i--) {
            char c = head.charAt(i);
            if (c == '\n' || c == '.' || c == '!' || c == '?') {
                best = i;
                break;
            }
        }
        return best >= 0 ? head.substring(0, best + 1).stripTrailing() : head + "…";
    }

    private static String cutLines(String s, int maxChars) {
        String[] lines = s.split("\n");
        StringBuilder kept = new StringBuilder();
        for (String line : lines) {
            int extra = kept.length() == 0 ? line.length() : line.length() + 1;
            if (kept.length() + extra > maxChars) break;
            if (kept.length() > 0) kept.append("\n");
            kept.append(line);
        }
        return kept.length() == 0 ? cutHead(lines[0], maxChars) : kept.toString();
    }

    private static String cutBlocks(String s, int maxChars) {
        String[] blocks = s.split("\n\n");
        StringBuilder kept = new StringBuilder();
        for (String block : blocks) {
            int extra = kept.length() == 0 ? block.length() : block.length() + 2;
            if (kept.length() + extra > maxChars) break;
            if (kept.length() > 0) kept.append("\n\n");
            kept.append(block);
        }
        return kept.length() == 0 ? cutHead(blocks[0], maxChars) : kept.toString();
    }
}
