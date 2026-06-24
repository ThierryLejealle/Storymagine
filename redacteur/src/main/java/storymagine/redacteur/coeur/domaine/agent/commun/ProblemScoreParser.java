package storymagine.redacteur.coeur.domaine.agent.commun;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses LLM responses that use the PROBLEME:/SCORE: format (dream, whatif, style, goal agents).
 * Also handles custom tag names via {@link #parseTagged(String, String)}.
 */
public final class ProblemScoreParser {

    private ProblemScoreParser() {}

    /** Extracts lines matching {@code "PROBLEME: ..."} from a raw LLM response. */
    public static List<String> parseProblems(String response) {
        return parseTagged(response, "PROBLEME");
    }

    /** Extracts lines matching {@code "<tag>: ..."} or {@code "<tag> : ..."} (case-insensitive). */
    public static List<String> parseTagged(String response, String tag) {
        List<String> result = new ArrayList<>();
        if (response == null) return result;
        String prefixStrict  = tag.toUpperCase() + ":";
        String prefixSpaced  = tag.toUpperCase() + " :";
        for (String line : response.split("\n")) {
            String t = line.trim();
            String norm = t.toUpperCase();
            int prefixLen;
            if      (norm.startsWith(prefixSpaced))  prefixLen = prefixSpaced.length();
            else if (norm.startsWith(prefixStrict))  prefixLen = prefixStrict.length();
            else continue;
            String value = t.substring(prefixLen).trim().replace("\"", "");
            if (!value.isBlank() && !value.equalsIgnoreCase("[RIEN]")) result.add(value);
        }
        return result;
    }

    /** Parses {@code "SCORE: N"} from a raw LLM response; returns 5.0 if not found. */
    public static double parseScore(String response) {
        if (response == null) return 5.0;
        for (String line : response.split("\n")) {
            String t = line.trim();
            if (t.startsWith("SCORE:")) {
                String raw = t.substring("SCORE:".length()).trim().replace("/10", "").trim();
                int sp = raw.indexOf(' ');
                if (sp >= 0) raw = raw.substring(0, sp);
                try { return Math.min(10.0, Math.max(0.0, Double.parseDouble(raw))); }
                catch (NumberFormatException ignored) {}
            }
        }
        return 5.0;
    }

    /** Parses {@code "SCORE: N"} as integer; returns 5 if not found. */
    public static int parseScoreInt(String response) {
        return (int) Math.round(parseScore(response));
    }
}
