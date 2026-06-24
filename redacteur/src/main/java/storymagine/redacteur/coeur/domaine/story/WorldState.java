package storymagine.redacteur.coeur.domaine.story;

import java.util.*;

/**
 * Mutable snapshot of the story world: entity states, recent events, and permanent plot directives.
 * Supports snapshot/restore for dream and what_if chapters that must not alter the main continuity.
 */
public class WorldState {

    private static final int MAX_EVENTS = 4;

    private final Map<String, String> entityStates   = new LinkedHashMap<>();
    private final List<String>        recentEvents   = new ArrayList<>();
    private final Map<String, String> plotDirectives = new LinkedHashMap<>();

    /** Merges extracted entity states (key = entity name, value = current state). */
    public void updateEntities(Map<String, String> updates) {
        entityStates.putAll(updates);
    }

    public void addRecentEvent(String event) {
        recentEvents.add(event);
        if (recentEvents.size() > MAX_EVENTS) recentEvents.remove(0);
    }

    /**
     * Applies author-declared plot directives. Syntax per entry:
     *   "Entity → state"  — add or update
     *   "-Entity"         — remove
     */
    public void applyPlotDirectives(List<String> directives) {
        if (directives == null) return;
        for (String d : directives) {
            String t = d.trim();
            if (t.startsWith("-")) {
                plotDirectives.remove(t.substring(1).trim());
            } else {
                int arrow = t.indexOf('→');
                if (arrow > 0) {
                    plotDirectives.put(t.substring(0, arrow).trim(), t.substring(arrow + 1).trim());
                }
            }
        }
    }

    /** Clears LLM-extracted state (entities + events) but preserves author plot directives. */
    public void clearExtractedState() {
        entityStates.clear();
        recentEvents.clear();
    }

    public Map<String, String> entityStates()   { return Map.copyOf(entityStates); }
    public List<String>        recentEvents()   { return List.copyOf(recentEvents); }
    public Map<String, String> plotDirectives() { return Map.copyOf(plotDirectives); }

    // ── Snapshot / restore — for dream and what_if chapter isolation ──────────

    public record Snapshot(
        Map<String, String> entityStates,
        List<String>        recentEvents,
        Map<String, String> plotDirectives
    ) {}

    public Snapshot snapshot() {
        return new Snapshot(
            new LinkedHashMap<>(entityStates),
            new ArrayList<>(recentEvents),
            new LinkedHashMap<>(plotDirectives)
        );
    }

    public void restore(Snapshot snap) {
        entityStates.clear();   entityStates.putAll(snap.entityStates());
        recentEvents.clear();   recentEvents.addAll(snap.recentEvents());
        plotDirectives.clear(); plotDirectives.putAll(snap.plotDirectives());
    }
}
