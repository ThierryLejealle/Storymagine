package storymagine.commun.coeur.domaine.text;

/**
 * Applies a single (search → replacement) text patch, tolerating quote-character
 * substitutions in the search phrase (LLM correctors often re-quote an embedded
 * citation with a different quote character than the source text uses at that
 * spot — same content, wrong glyph). Also falls back to stripping a trailing
 * parenthetical comment the LLM sometimes appends to its own search phrase
 * (e.g. citing "...idyllique." (Répétition structurelle)" when the source text
 * never had that annotation) — tried only if the full phrase doesn't match, so
 * a search phrase that legitimately needs its trailing annotation to match the
 * source text (and remove it) still matches on the first attempt. Knows nothing
 * about the caller's domain — only about text, quote characters and comments.
 */
public final class TextPatcher {

    private static final String QUOTE_CHARS = "\"'“”‘’";

    private TextPatcher() {
    }

    /** Outcome of one patch attempt: the resulting text, and whether a match was found. */
    public record Result(String text, boolean applied) {
    }

    /**
     * Replaces the first occurrence of search in text with replacement. Matching treats
     * any two quote-family characters (", ', “, ”, ‘, ’) as equal to each other — every
     * other character must match exactly. If search isn't found as-is, retries once with
     * a trailing "(...)" comment stripped from search. Returns the original text with
     * applied=false if still not found.
     */
    public static Result apply(String text, String search, String replacement) {
        Result direct = tryMatch(text, search, replacement);
        if (direct != null) return direct;

        String shorter = stripTrailingQuotes(stripTrailingComment(search));
        if (!shorter.equals(search)) {
            Result fallback = tryMatch(text, shorter, replacement);
            if (fallback != null) return fallback;
        }
        return new Result(text, false);
    }

    private static Result tryMatch(String text, String search, String replacement) {
        int idx = quoteTolerantIndexOf(text, search);
        if (idx < 0) return null;
        String patched = text.substring(0, idx) + replacement + text.substring(idx + search.length());
        return new Result(patched, true);
    }

    private static String stripTrailingComment(String s) {
        return s.replaceAll("\\s*\\([^()]*\\)$", "");
    }

    /** Strips a quote character left exposed at the end once a trailing comment is removed. */
    private static String stripTrailingQuotes(String s) {
        int end = s.length();
        while (end > 0 && isQuote(s.charAt(end - 1))) end--;
        return s.substring(0, end);
    }

    private static int quoteTolerantIndexOf(String text, String search) {
        if (search.isEmpty()) return -1;
        int max = text.length() - search.length();
        for (int start = 0; start <= max; start++) {
            if (matchesAt(text, start, search)) return start;
        }
        return -1;
    }

    private static boolean matchesAt(String text, int start, String search) {
        for (int i = 0; i < search.length(); i++) {
            char a = text.charAt(start + i);
            char b = search.charAt(i);
            if (a == b) continue;
            if (isQuote(a) && isQuote(b)) continue;
            return false;
        }
        return true;
    }

    private static boolean isQuote(char c) {
        return QUOTE_CHARS.indexOf(c) >= 0;
    }
}
