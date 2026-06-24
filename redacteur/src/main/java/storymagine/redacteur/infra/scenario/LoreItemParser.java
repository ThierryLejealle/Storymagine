package storymagine.redacteur.infra.scenario;

import storymagine.redacteur.coeur.domaine.scenario.lore.LoreInline;
import storymagine.redacteur.coeur.domaine.scenario.lore.LoreItem;
import storymagine.redacteur.coeur.domaine.scenario.lore.LorePool;
import storymagine.redacteur.coeur.domaine.scenario.lore.LoreRef;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses the mixed lore directive found in chapter YAML fields.
 * Handles: ["TAG"]  → LoreRef   and   "text" or plain text → LoreInline
 */
class LoreItemParser {

    static List<LoreItem> parse(String raw, LorePool pool) {
        if (raw == null || raw.isBlank()) return List.of();

        List<LoreItem> items = new ArrayList<>();
        for (String line : raw.split("\n")) {
            String t = line.strip();
            if (t.isBlank()) continue;

            String tag = extractTag(t);
            if (tag != null) {
                var resolved = pool.find(tag);
                items.add(new LoreRef(tag, resolved.orElse(null)));
            } else {
                String text = t.replaceAll("^\"|\"$", "").strip();
                if (!text.isBlank()) items.add(new LoreInline(text));
            }
        }
        return List.copyOf(items);
    }

    /** Returns the tag name if the token is ["TAG"] or [TAG], else null. */
    private static String extractTag(String token) {
        if (token.startsWith("[") && token.contains("]")) {
            String inner = token.substring(1, token.indexOf(']')).replaceAll("\"", "").trim();
            if (!inner.isBlank()) return inner;
        }
        return null;
    }
}
