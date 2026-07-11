package storymagine.redacteur.coeur.domaine.agent.commun;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses tiered critic output (AMELIORATION / DEFAUT_SIGNIFICATIF / DEFAUT_MAJEUR)
 * and computes a score from the tier counts.
 * Used by most Critic agents in the project — chapter critics (ChapterNarrativeCritic,
 * ChapterCoherenceCritic...), the 5 plan critics (PlanGoalCritic, PlanFactsCritic,
 * PlanCoherenceCritic, PlanContinuityCritic, PlanDramaCritic), and the story-plan critics.
 *
 * Tolerates two conventions for listing several problems under the same tier — a small LLM
 * may legitimately produce either:
 *   1. One full "TAG: finding" line per problem, tag repeated each time (historical convention,
 *      still used by most existing critics).
 *   2. A single "TAG:" header line (empty after the colon) followed by one or more "- finding"
 *      lines below it, until the next tag or the end of the response (the format several plan
 *      critics were given in 2026-07-11 — a real production bug: a bare "TAG:" line has empty
 *      inline content and was silently treated as [RIEN], every finding under it was dropped,
 *      scores stayed at 10).
 * A tag line that already carries content on its own line (a finding or [RIEN]) is considered
 * fully closed — it does NOT await further bullets. This matters: without it, any trailing text
 * after the last tier (e.g. a stray "SCORE:" line some callers still send) would be silently
 * miscounted as one more finding of the last tier.
 * See CriticOutputParserTest for both conventions exercised explicitly, including a regression
 * test built from the exact response that triggered the bug.
 */
public final class CriticOutputParser {

    private static final List<String> PROBLEM_TAGS =
        List.of("PROBLEME:", "DEFAUT_MAJEUR:", "DEFAUT_SIGNIFICATIF:", "AMELIORATION:");
    private static final List<String> SCORE_TAGS =
        List.of("AMELIORATION:", "DEFAUT_SIGNIFICATIF:", "DEFAUT_MAJEUR:");

    private CriticOutputParser() {}

    /** Extracts all non-empty problem lines from a tiered critic response — both list conventions. */
    public static List<String> parseProblems(String response) {
        List<String> problems = new ArrayList<>();
        if (response == null) return problems;
        boolean awaitingBullets = false;
        for (String line : normalize(response).split("\n")) {
            String t = line.trim();
            if (t.isEmpty()) continue;
            String normUpper = Normalizer.normalize(t, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}", "")
                .toUpperCase();
            String tag = startsWithAny(normUpper, PROBLEM_TAGS);
            if (tag != null) {
                String p = stripBullet(t.substring(t.indexOf(':') + 1).trim());
                if (p.isEmpty()) {
                    awaitingBullets = true;
                } else {
                    awaitingBullets = false;
                    if (!isSentinelText(p)) problems.add(p);
                }
            } else if (awaitingBullets) {
                String p = stripBullet(t);
                if (isSentinelText(p)) {
                    awaitingBullets = false; // "[RIEN]" closes the section, no more bullets expected
                } else {
                    problems.add(p);
                }
            }
        }
        return problems;
    }

    /**
     * Computes a score from tier counts (AMELIORATION / DEFAUT_SIGNIFICATIF / DEFAUT_MAJEUR).
     * Does NOT use a SCORE: line — score is derived from the tiers.
     */
    public static double calculateScore(String response) {
        if (response == null) return 5.0;
        int amelioration = 0, significatif = 0, majeur = 0;
        String awaitingTag = null;
        for (String line : normalize(response).split("\n")) {
            String t = line.trim().toUpperCase();
            if (t.isEmpty()) continue;
            String tag = startsWithAny(t, SCORE_TAGS);
            String content;
            if (tag != null) {
                content = stripBullet(t.substring(t.indexOf(':') + 1).trim()).replaceAll("[\\[\\]().]", "").trim();
                awaitingTag = content.isEmpty() ? tag : null;
            } else if (awaitingTag != null) {
                tag = awaitingTag;
                content = stripBullet(t).replaceAll("[\\[\\]().]", "").trim();
                if (isSentinel(content)) {
                    awaitingTag = null; // "[RIEN]" closes the section, no more bullets expected
                    continue;
                }
            } else {
                continue;
            }
            if (isSentinel(content)) continue;
            if      (tag.equals("AMELIORATION:"))        amelioration++;
            else if (tag.equals("DEFAUT_SIGNIFICATIF:")) significatif++;
            else if (tag.equals("DEFAUT_MAJEUR:"))       majeur++;
        }
        if (majeur > 0) {
            double sigCorr  = Math.min(significatif * 0.05, 0.45);
            double amelCorr = Math.min(amelioration * 0.005, 0.05);
            return Math.max(1.0, majBase(majeur) - sigCorr - amelCorr);
        }
        if (significatif > 0) {
            double amelCorr = Math.min(amelioration * 0.05, 0.45);
            return Math.max(4.0, sigBase(significatif) - amelCorr);
        }
        return amelBase(amelioration);
    }

    private static String startsWithAny(String upperLine, List<String> tags) {
        for (String tag : tags) {
            if (upperLine.startsWith(tag)) return tag;
        }
        return null;
    }

    /** Strips a leading "- ", "* " or "• " bullet marker, if present. */
    private static String stripBullet(String s) {
        return s.replaceFirst("^[-*•]\\s*", "").trim();
    }

    private static boolean isSentinelText(String p) {
        String pn = p.toLowerCase().replaceAll("[\\[\\]().]", "").trim();
        return isSentinel(pn.toUpperCase());
    }

    /** Returns true when the content (uppercase, stripped of []().) is a "nothing to report" sentinel. */
    private static boolean isSentinel(String s) {
        return s.isEmpty()
            || s.equals("AUCUN") || s.equals("AUCUNE")
            || s.equals("RIEN")  || s.equals("NONE") || s.equals("NEANT")
            || s.startsWith("AUCUN ")  || s.startsWith("AUCUNE ")
            || s.startsWith("RIEN ")   || s.startsWith("NONE ") || s.startsWith("NEANT ");
    }

    private static String normalize(String response) {
        String r = response.replaceAll("(?i)(AMELIORATION|DEFAUT_SIGNIFICATIF|DEFAUT_MAJEUR|PROBLEME)\\s*:", "$1:");
        r = r.replaceAll("(?i)(AMELIORATION:|DEFAUT_SIGNIFICATIF:|DEFAUT_MAJEUR:|PROBLEME:)", "\n$1");
        return r.replaceAll("\n{2,}", "\n").trim();
    }

    private static double amelBase(int n) {
        if (n <= 0) return 10.0;
        if (n <= 3) return 10.0 - n * 0.5;
        if (n < 10) return 8.5 - (n - 3) * (1.5 / 7.0);
        return 7.0;
    }

    private static double sigBase(int n) {
        if (n <= 3) return 7.0 - n * 0.5;
        if (n < 10) return 5.5 - (n - 3) * (1.0 / 7.0);
        return 4.5;
    }

    private static double majBase(int n) {
        if (n <= 3) return 4.0 - n * 0.5;
        if (n < 10) return 2.5 - (n - 3) * (1.0 / 7.0);
        return 1.5;
    }
}
