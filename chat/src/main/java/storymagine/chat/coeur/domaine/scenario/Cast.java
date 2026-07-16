package storymagine.chat.coeur.domaine.scenario;

import java.util.List;
import java.util.Optional;

/**
 * The full roster of every Npc a scenario can play — not session presence (see ChatSession for
 * which of them are currently active in the scene). npcs() preserves a stable order (the order
 * given at construction, expected to be alphabetical by id from file discovery) : callers that
 * need determinism across calls, like SpeakerSelector's rotation fallback, rely on it.
 */
public final class Cast {

    private final List<Npc> npcs;

    public Cast(List<Npc> npcs) {
        this.npcs = List.copyOf(npcs);
    }

    public List<Npc> npcs() { return npcs; }

    public Optional<Npc> find(String id) {
        return npcs.stream().filter(n -> n.id().equals(id)).findFirst();
    }

    public int size() { return npcs.size(); }
}
