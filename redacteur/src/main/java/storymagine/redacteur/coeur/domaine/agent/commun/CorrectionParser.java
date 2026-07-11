package storymagine.redacteur.coeur.domaine.agent.commun;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Parses LLM responses that use the FAUX:/JUSTE: correction format (all Corrector agents).
 * Cleans stray leading/trailing quote characters from both sides identically, and strips
 * LLM-invented trailing parenthetical commentary from JUSTE only — a corrected phrase must
 * never carry a trailing comment, whereas a wrong phrase legitimately can (it may need to
 * reproduce a source-text annotation in order to remove it).
 */
public final class CorrectionParser {

    private static final String QUOTE_CHARS = "\"'“”‘’";

    private CorrectionParser() {
    }

    /**
     * Extracts (FAUX, JUSTE) pairs from a raw response, building each result via factory.
     * Returns an empty list if the response starts with noCorrectionSentinel (e.g.
     * "PAS DE CORRECTION", "PAS D'ERREUR").
     */
    public static <T> List<T> parse(String response, String noCorrectionSentinel,
                                     BiFunction<String, String, T> factory) {
        List<T> result = new ArrayList<>();
        if (response == null || response.trim().startsWith(noCorrectionSentinel)) return result;
        String wrong = null;
        for (String line : response.split("\n")) {
            String t = line.trim();
            if (t.startsWith("- FAUX:") || t.startsWith("FAUX:")) {
                wrong = sanitize(t.replaceFirst("^-?\\s*FAUX:", "").trim());
            } else if (t.startsWith("JUSTE:") && wrong != null) {
                String correct = stripTrailingComment(sanitize(t.substring("JUSTE:".length()).trim()));
                if (!wrong.isBlank() && !correct.isBlank())
                    result.add(factory.apply(wrong, correct));
                wrong = null;
            }
        }
        return result;
    }

    /** Strips every leading and trailing quote-family character (", ', “, ”, ‘, ’). */
    private static String sanitize(String s) {
        int start = 0;
        int end   = s.length();
        while (start < end && isQuote(s.charAt(start))) start++;
        while (end > start && isQuote(s.charAt(end - 1))) end--;
        return s.substring(start, end).trim();
    }

    /** Strips a trailing "(...)" annotation the LLM appended after the citation. */
    private static String stripTrailingComment(String s) {
        return s.replaceAll("\\s*\\([^()]*\\)$", "");
    }

    private static boolean isQuote(char c) {
        return QUOTE_CHARS.indexOf(c) >= 0;
    }
}
