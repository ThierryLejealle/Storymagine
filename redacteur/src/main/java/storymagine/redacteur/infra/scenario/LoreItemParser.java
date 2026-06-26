package storymagine.redacteur.infra.scenario;

import storymagine.redacteur.coeur.domaine.scenario.lore.LoreInline;
import storymagine.redacteur.coeur.domaine.scenario.lore.LoreItem;
import storymagine.redacteur.coeur.domaine.scenario.lore.LorePool;
import storymagine.redacteur.coeur.domaine.scenario.lore.LoreRef;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Parses the mixed lore directive found in chapter YAML fields.
 * Handles: ["TAG"]  or ["TAG1","TAG2"] → LoreRef(s)   and   "text" or plain text → LoreInline
 */
class LoreItemParser {

    static List<LoreItem> parse(String raw, LorePool pool) {
        if (raw == null || raw.isBlank()) return List.of();

        List<LoreItem> items = new ArrayList<>();
        for (String line : raw.split("\n")) {
            String t = line.strip();
            if (t.isBlank()) continue;

            List<String> tags = extractTags(t);
            if (!tags.isEmpty()) {
                for (String tag : tags) {
                    var resolved = pool.find(tag);
                    items.add(new LoreRef(tag, resolved.orElse(null)));
                }
            } else {
                String text = t.replaceAll("^\"|\"$", "").strip();
                if (!text.isBlank()) items.add(new LoreInline(text));
            }
        }
        return List.copyOf(items);
    }

    /**
     * Returns tag names if the token starts with [...].
     * Supports ["TAG"] and ["TAG1", "TAG2", ...].
     */
    private static List<String> extractTags(String token) {
        if (!token.startsWith("[") || !token.contains("]")) return List.of();
        String inner = token.substring(1, token.indexOf(']')).replaceAll("\"", "").trim();
        if (inner.isBlank()) return List.of();
        return Arrays.stream(inner.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }
}
