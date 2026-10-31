package storymagine.chat.coeur.domaine.activity;

import storymagine.chat.coeur.domaine.scenario.Cast;
import storymagine.chat.coeur.domaine.scenario.Npc;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * The activity of every Npc of the Cast at one point of the story, in Cast order : level and where
 * it comes from. Immutable ; an id the Cast does not know is simply present, implicit.
 */
public final class CastActivity {

    private final Map<String, NpcActivityState> states;

    private CastActivity(Map<String, NpcActivityState> states) {
        this.states = states;
    }

    /** Everybody present, implicit — the state before any command. */
    public static CastActivity defaults(Cast cast) {
        Map<String, NpcActivityState> states = new LinkedHashMap<>();
        for (Npc npc : cast.npcs()) states.put(npc.id(), NpcActivityState.DEFAULT);
        return new CastActivity(states);
    }

    public NpcActivityState stateOf(String npcId) { return states.getOrDefault(npcId, NpcActivityState.DEFAULT); }

    public NpcActivity levelOf(String npcId) { return stateOf(npcId).level(); }

    public CastActivity with(String npcId, NpcActivityState state) {
        Map<String, NpcActivityState> next = new LinkedHashMap<>(states);
        next.put(npcId, state);
        return new CastActivity(next);
    }

    /** Ids whose level is not ABSENT, in Cast order. */
    public Set<String> inSceneIds() {
        Set<String> ids = new LinkedHashSet<>();
        states.forEach((id, state) -> { if (state.level().isInScene()) ids.add(id); });
        return ids;
    }

    public Map<String, NpcActivityState> states() { return Map.copyOf(states); }
}
