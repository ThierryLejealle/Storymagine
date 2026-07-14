package storymagine.redacteur.coeur.domaine.orchestrator.common;

import storymagine.redacteur.coeur.domaine.story.WorldState;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/** Static helpers: Story domain objects → LLM-ready strings, and reverse parsing. */
public final class StoryFormatters {

    private StoryFormatters() {}

    /** Formats entity states as "Entity : state" lines. */
    public static String entityState(WorldState ws) {
        Map<String, String> states = ws.entityStates();
        if (states.isEmpty()) return "";
        return states.entrySet().stream()
                .map(e -> e.getKey() + " : " + e.getValue())
                .collect(Collectors.joining("\n"));
    }

    /**
     * Parses StateExtractor output lines and applies them to WorldState.
     * Line format: "ETAT: Entity → state" or "EVENT: event description" — same arrow syntax
     * as WorldState.applyPlotDirectives.
     */
    public static void applyStateLines(WorldState ws, String stateLines) {
        if (stateLines == null || stateLines.isBlank()) return;
        Map<String, String> entities = new LinkedHashMap<>();
        for (String line : stateLines.split("\n")) {
            String t = line.trim();
            if (t.startsWith("ETAT:")) {
                String content = t.substring(5).trim();
                int arrow = content.indexOf('→');
                if (arrow > 0) {
                    entities.put(content.substring(0, arrow).trim(), content.substring(arrow + 1).trim());
                }
            } else if (t.startsWith("EVENT:")) {
                ws.addRecentEvent(t.substring(6).trim());
            }
        }
        if (!entities.isEmpty()) ws.updateEntities(entities);
    }
}
