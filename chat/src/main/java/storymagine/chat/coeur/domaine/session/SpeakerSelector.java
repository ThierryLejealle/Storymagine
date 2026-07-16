package storymagine.chat.coeur.domaine.session;

import storymagine.chat.coeur.domaine.scenario.Cast;
import storymagine.chat.coeur.domaine.scenario.Npc;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Decides which present Npc(s) answer the player's message, in three steps :
 * 1. Every present Npc mentioned by name answers — ALL of them if several are named, in the order
 *    their name first appears in the message (not alphabetical : the player addressed them in that
 *    order, and a small model gets visibly confused if a merely name-dropped Npc answers before the
 *    one actually spoken to). Matching is forgiving : accents/case stripped, and only the first
 *    MENTION_PREFIX_LENGTH characters of each name word need to match at a word boundary — a typo
 *    or a shortened name ("Cel" for "Céleste") still counts, same spirit as SillyTavern's group
 *    chats.
 * 2. Nobody named : if exactly one Npc spoke in the previous round, they answer again — a bare
 *    follow-up ("et après ?") continues the conversation with whoever was just talking rather than
 *    picking someone at random.
 * 3. Still nobody resolved (first exchange, or the previous round had 0 or 2+ speakers) : one
 *    present, interjection-eligible Npc is picked at random (see rollInterjectors below for why
 *    eligibility matters here too — the 💬 toggle's "only reacts if named" promise must hold for
 *    every path that doesn't name someone, not just this one).
 *
 * rollInterjectors is a separate step, always applied after select() regardless of which of the
 * three paths resolved the primary speaker(s) : every other present, interjection-eligible Npc
 * gets an independent roll to react anyway, in random order (see evols).
 */
public final class SpeakerSelector {

    private SpeakerSelector() {}

    private static final int MENTION_PREFIX_LENGTH = 5;

    /**
     * presentIds must not be empty — ChatSession.setPresent guards against muting the last
     * present Npc. previousRoundSpeakerIds is who answered the round before this message (empty
     * on the very first exchange) — see ChatServiceImpl for how it's derived from session.turns().
     */
    public static List<Npc> select(Cast cast, Set<String> presentIds, Set<String> interjectingIds,
                                    String playerMessage, Set<String> previousRoundSpeakerIds, Random random) {
        List<Npc> present = presentSorted(cast, presentIds);
        List<Npc> mentioned = mentionedAmong(present, playerMessage);
        if (!mentioned.isEmpty()) return mentioned;

        if (previousRoundSpeakerIds.size() == 1) {
            String onlyId = previousRoundSpeakerIds.iterator().next();
            Optional<Npc> continuing = present.stream().filter(n -> n.id().equals(onlyId)).findFirst();
            if (continuing.isPresent()) return List.of(continuing.get());
        }

        List<Npc> eligible = present.stream().filter(n -> interjectingIds.contains(n.id())).toList();
        List<Npc> pool = eligible.isEmpty() ? present : eligible;
        List<Npc> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled, random);
        return List.of(shuffled.get(0));
    }

    /**
     * Every present, interjection-eligible Npc other than the primary speaker(s) gets an
     * independent roll to react anyway — applied regardless of how primarySpeakers was resolved
     * (a mention, a continuation, or a random pick), and regardless of how many primary speakers
     * there are. Result order is randomized (not present-sorted) : nothing about who happens to
     * react unprompted should read as more "senior" than another.
     */
    public static List<Npc> rollInterjectors(Cast cast, Set<String> presentIds, Set<String> interjectingIds,
                                              List<Npc> primarySpeakers, double chance, Random random) {
        List<Npc> present = presentSorted(cast, presentIds);
        Set<String> primaryIds = primarySpeakers.stream().map(Npc::id).collect(Collectors.toSet());

        List<Npc> interjectors = new ArrayList<>();
        for (Npc candidate : present) {
            if (primaryIds.contains(candidate.id())) continue;
            if (!interjectingIds.contains(candidate.id())) continue;
            if (random.nextDouble() < chance) interjectors.add(candidate);
        }
        Collections.shuffle(interjectors, random);
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

    /** Present Npcs mentioned in playerMessage, ordered by where their name FIRST appears in it. */
    private static List<Npc> mentionedAmong(List<Npc> present, String playerMessage) {
        record Mention(Npc npc, int position) {}
        return present.stream()
            .map(n -> new Mention(n, firstMentionPosition(playerMessage, n.name())))
            .filter(m -> m.position() >= 0)
            .sorted(Comparator.comparingInt(Mention::position))
            .map(Mention::npc)
            .toList();
    }

    /**
     * Index in text of the earliest word starting with (a normalized prefix of) any word of name,
     * or -1 if none matches. Accents and case are stripped on both sides ; only the first
     * MENTION_PREFIX_LENGTH characters of each name word need to match, so "Cel" or a typo like
     * "Celestre" still resolves to "Céleste". Normalizer.normalize + stripping combining marks
     * preserves string length for the accented Latin characters this project's content uses, so
     * positions found in the normalized text line up with the original text.
     */
    private static int firstMentionPosition(String text, String name) {
        if (name == null || name.isBlank() || text == null) return -1;
        String normalizedText = normalize(text);
        int earliest = -1;
        for (String word : name.trim().split("\\s+")) {
            if (word.isBlank()) continue;
            String normalizedWord = normalize(word);
            String prefix = normalizedWord.substring(0, Math.min(MENTION_PREFIX_LENGTH, normalizedWord.length()));
            Matcher m = Pattern.compile("\\b" + Pattern.quote(prefix)).matcher(normalizedText);
            if (m.find()) {
                int pos = m.start();
                if (earliest < 0 || pos < earliest) earliest = pos;
            }
        }
        return earliest;
    }

    private static String normalize(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase();
    }
}
