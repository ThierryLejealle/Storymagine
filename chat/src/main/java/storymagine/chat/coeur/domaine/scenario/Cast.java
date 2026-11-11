package storymagine.chat.coeur.domaine.scenario;

import java.text.Normalizer;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The full roster of every Npc a scenario can play — not session presence (see ChatSession for
 * which of them are currently active in the scene). npcs() preserves a stable order (the order
 * given at construction, expected to be alphabetical by id from file discovery) : callers that
 * need determinism across calls, like SpeakerSelector's rotation fallback, rely on it.
 * templateIds are the ids of the sheets used as templates by "{npc-arrives template Name}" : they are
 * not Npcs of the Cast (the characters created from them are, see CastInstantiator).
 */
public final class Cast {

    private final List<Npc> npcs;
    private final Set<String> templateIds;

    public Cast(List<Npc> npcs) {
        this(npcs, Set.of());
    }

    public Cast(List<Npc> npcs, Set<String> templateIds) {
        this.npcs = List.copyOf(npcs);
        this.templateIds = Set.copyOf(templateIds);
    }

    /** True if `id` is a sheet used as a template (case ignored) : not a character, a model to create some from. */
    public boolean isTemplate(String id) {
        return templateIds.stream().anyMatch(t -> t.equalsIgnoreCase(id == null ? "" : id.strip()));
    }

    public Set<String> templateIds() { return templateIds; }

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

    /** Every Npc the name could mean (see findByName) : more than one means the name is ambiguous. */
    public List<Npc> findAllByName(String name) {
        String wanted = normalize(name);
        if (wanted.isEmpty()) return List.of();
        return npcs.stream().filter(n -> matches(n, wanted)).toList();
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
