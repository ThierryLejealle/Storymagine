package storymagine.chat.coeur.domaine.agent.commun;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses the shared "ISSUES:"/"SUGGESTIONS:" output format used by ScenarioContinuityReviewer and
 * ScenarioClarityReviewer — two bullet-list sections, "[RIEN]" as the sentinel for an empty one
 * (same convention as redacteur's PlanGoalCritic).
 */
public final class ReviewOutputParser {

    private static final String ISSUES_HEADER      = "ISSUES:";
    private static final String SUGGESTIONS_HEADER = "SUGGESTIONS:";
    private static final String EMPTY_SENTINEL     = "[RIEN]";

    private ReviewOutputParser() {}

    public record Review(List<String> issues, List<String> suggestions) {}

    public static Review parse(String raw) {
        if (raw == null || raw.isBlank()) return new Review(List.of(), List.of());

        List<String> issues      = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();
        List<String> current     = null;

        for (String line : raw.split("\n")) {
            String trimmed = line.strip();
            if (trimmed.equalsIgnoreCase(ISSUES_HEADER))      { current = issues;      continue; }
            if (trimmed.equalsIgnoreCase(SUGGESTIONS_HEADER)) { current = suggestions; continue; }
            if (current == null || trimmed.isEmpty() || trimmed.equalsIgnoreCase(EMPTY_SENTINEL)) continue;
            current.add(trimmed.startsWith("- ") ? trimmed.substring(2).strip() : trimmed);
        }
        return new Review(List.copyOf(issues), List.copyOf(suggestions));
    }
}
