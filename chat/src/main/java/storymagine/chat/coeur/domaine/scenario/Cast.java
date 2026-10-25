package storymagine.chat.coeur.domaine.scenario;

import java.text.Normalizer;
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

    /**
     * The Npc an author means by name (see NpcOpening) : id, full name, or any single word of the
     * name ("Elena" finds "Elena Voss"), ignoring case and accents. Empty if nobody matches.
     */
    public Optional<Npc> findByName(String name) {
        String wanted = normalize(name);
        if (wanted.isEmpty()) return Optional.empty();
        return npcs.stream().filter(n -> matches(n, wanted)).findFirst();
    }

    private static boolean matches(Npc npc, String wanted) {
        if (normalize(npc.id()).equals(wanted) || normalize(npc.name()).equals(wanted)) return true;
        for (String word : normalize(npc.name()).split("\\s+")) {
            if (word.equals(wanted)) return true;
        }
        return false;
    }

    private static String normalize(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase().strip();
    }

    public int size() { return npcs.size(); }
}
