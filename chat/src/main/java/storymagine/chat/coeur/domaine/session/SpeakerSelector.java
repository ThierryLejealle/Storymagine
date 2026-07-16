package storymagine.chat.coeur.domaine.session;

import storymagine.chat.coeur.domaine.scenario.Cast;
import storymagine.chat.coeur.domaine.scenario.Npc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Decides which present Npc(s) answer the player's message : every present Npc whose name is
 * mentioned as a whole word (matching any single word of a multi-word name, e.g. "Elena" mentions
 * "Elena Voss" — same principle SillyTavern's group chats use) answers, in stable alphabetical-by-
 * id order — deliberately not the order they were mentioned in the text, to keep this a pure,
 * easily-tested function rather than parsing mention positions for a marginal gain. If nobody is
 * mentioned, two present Npcs are picked at random (one if only one is present). See the multi-NPC
 * plan (evols + project memory) for why this is deliberately simpler than SillyTavern (no
 * talkativeness weighting, no manual override yet). The random fallback only draws from
 * interjection-eligible Npcs (see interjectingIds) — the same 💬 toggle that governs
 * rollInterjectors also has to hold when nobody was named at all, or its "only reacts if named"
 * promise (see the chat UI's tooltip) would be broken for exactly that case. rollInterjectors is
 * a separate step, layered on top of a select() result : when exactly one Npc was addressed by
 * name, every other present, interjection-eligible Npc gets an independent roll to react anyway
 * (see evols).
 */
public final class SpeakerSelector {

    private SpeakerSelector() {}

    private static final int RANDOM_FALLBACK_COUNT = 2;

    /**
     * presentIds must not be empty — ChatSession.setPresent guards against muting the last
     * present Npc. If nobody is named, the random fallback draws only from Npcs both present and
     * interjection-eligible ; if that leaves nobody (every present Npc opted out), it falls back
     * to every present Npc instead — someone has to answer the player's message.
     */
    public static List<Npc> select(Cast cast, Set<String> presentIds, Set<String> interjectingIds,
                                    String playerMessage, Random random) {
        List<Npc> present = presentSorted(cast, presentIds);
        List<Npc> mentioned = mentionedAmong(present, playerMessage);
        if (!mentioned.isEmpty()) return mentioned;

        List<Npc> eligible = present.stream().filter(n -> interjectingIds.contains(n.id())).toList();
        List<Npc> pool = eligible.isEmpty() ? present : eligible;

        int count = Math.min(RANDOM_FALLBACK_COUNT, pool.size());
        List<Npc> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled, random);
        return List.copyOf(shuffled.subList(0, count));
    }

    /**
     * Npcs who react unprompted this round even though they were not addressed — only when
     * exactly one present Npc was explicitly mentioned by name (primarySpeakers came from a
     * mention, not the random fallback ; a multi-mention or fallback round never triggers this,
     * see the feature discussion in evols). Each present, interjection-eligible Npc other than the
     * addressed one rolls independently against chance — so anywhere from none to all of them may
     * end up reacting the same round. Order is present-sorted (stable, same convention as select).
     */
    public static List<Npc> rollInterjectors(Cast cast, Set<String> presentIds, Set<String> interjectingIds,
                                              List<Npc> primarySpeakers, String playerMessage, double chance,
                                              Random random) {
        if (primarySpeakers.size() != 1) return List.of();
        List<Npc> present = presentSorted(cast, presentIds);
        List<Npc> mentioned = mentionedAmong(present, playerMessage);
        if (mentioned.size() != 1) return List.of(); // le primaire vient du repli aleatoire, pas d'une mention

        String targetId = primarySpeakers.get(0).id();
        List<Npc> interjectors = new ArrayList<>();
        for (Npc candidate : present) {
            if (candidate.id().equals(targetId)) continue;
            if (!interjectingIds.contains(candidate.id())) continue;
            if (random.nextDouble() < chance) interjectors.add(candidate);
        }
        return List.copyOf(interjectors);
    }

    private static List<Npc> presentSorted(Cast cast, Set<String> presentIds) {
        List<Npc> present = cast.npcs().stream()
            .filter(n -> presentIds.contains(n.id()))
            .sorted(Comparator.comparing(Npc::id))
            .toList();
        if (present.isEmpty()) {
            throw new IllegalStateException("Aucun personnage present pour repondre.");
        }
        return present;
    }

    private static List<Npc> mentionedAmong(List<Npc> present, String playerMessage) {
        return present.stream().filter(n -> mentionsWholeWord(playerMessage, n.name())).toList();
    }

    /** True if any single word of name appears as a whole word in text (case-insensitive). */
    private static boolean mentionsWholeWord(String text, String name) {
        if (name == null || name.isBlank() || text == null) return false;
        for (String word : name.trim().split("\\s+")) {
            if (word.isBlank()) continue;
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(word) + "\\b", Pattern.CASE_INSENSITIVE);
            if (pattern.matcher(text).find()) return true;
        }
        return false;
    }
}
