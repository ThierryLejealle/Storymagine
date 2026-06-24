package storymagine.redacteur.coeur.domaine.agent.commun;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses tiered critic output (AMELIORATION / DEFAUT_SIGNIFICATIF / DEFAUT_MAJEUR)
 * and computes a score from the tier counts.
 * Used by the four main critics: TextNarrativeCritic, TextCoherenceCritic,
 * PlanNarrativeCritic, PlanCoherenceCritic.
 */
public final class CriticOutputParser {

    private CriticOutputParser() {}

    /** Extracts all non-empty problem lines from a tiered critic response. */
    public static List<String> parseProblems(String response) {
        List<String> problems = new ArrayList<>();
        if (response == null) return problems;
        for (String line : normalize(response).split("\n")) {
            String t = line.trim();
            String norm = Normalizer.normalize(t, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}", "")
                .toUpperCase();
            if (norm.startsWith("PROBLEME:") || norm.startsWith("DEFAUT_") || norm.startsWith("AMELIORATION:")) {
                String p = t.substring(t.indexOf(':') + 1).trim();
                String pn = p.toLowerCase().replaceAll("[\\[\\]().]", "").trim();
                if (!pn.isEmpty() && !pn.equals("aucun") && !pn.equals("rien")
                        && !pn.equals("none") && !pn.equals("neant")) {
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
        for (String line : normalize(response).split("\n")) {
            String t = line.trim().toUpperCase();
            if (!t.startsWith("AMELIORATION:") && !t.startsWith("DEFAUT_SIGNIFICATIF:")
                    && !t.startsWith("DEFAUT_MAJEUR:")) continue;
            String content = t.substring(t.indexOf(':') + 1).replaceAll("[\\[\\]().]", "").trim();
            if (content.isEmpty() || content.equals("AUCUN") || content.equals("RIEN")
                    || content.equals("NONE") || content.equals("NEANT")) continue;
            if      (t.startsWith("AMELIORATION:"))        amelioration++;
            else if (t.startsWith("DEFAUT_SIGNIFICATIF:")) significatif++;
            else if (t.startsWith("DEFAUT_MAJEUR:"))       majeur++;
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
