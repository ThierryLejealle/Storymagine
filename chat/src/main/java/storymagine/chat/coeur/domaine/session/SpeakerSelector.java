package storymagine.chat.coeur.domaine.session;

import storymagine.chat.coeur.domaine.activity.ActivityResolver;
import storymagine.chat.coeur.domaine.activity.CastActivity;
import storymagine.chat.coeur.domaine.activity.NpcActivity;
import storymagine.chat.coeur.domaine.scenario.Cast;
import storymagine.chat.coeur.domaine.scenario.Npc;
import storymagine.chat.coeur.domaine.scenario.NpcOpening;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Decides which Npc(s) answer the player's message, from each Npc's activity (see NpcActivity) :
 * 1. Every ACTIVE Npc answers, at every exchange.
 * 2. Every ACTIVE or PRESENT Npc mentioned by name in the last exchange answers too — in the
 *    player's message, or in a reply of the previous round (J1 talked about J2, J2 may answer).
 *    They answer in the order their name first appears, the player's message first : the player
 *    addressed them in that order, and a small model gets visibly confused if a merely name-dropped
 *    Npc answers before the one actually spoken to. Matching is forgiving : accents/case stripped,
 *    and only the first MENTION_PREFIX_LENGTH characters of each name word need to match at a word
 *    boundary — a typo or a shortened name ("Cel" for "Céleste") still counts, same spirit as
 *    SillyTavern's group chats. Mentioned Npcs come before the other active ones.
 * 3. Nobody active and nobody mentioned : if exactly one Npc spoke in the previous round, they
 *    answer again — a bare follow-up ("et après ?") continues the conversation with whoever was just
 *    talking rather than picking someone at random.
 * 4. Still nobody (first exchange, or the previous round had 0 or 2+ speakers) : one PRESENT or
 *    PASSIVE Npc is picked at random, a PRESENT one twice as likely as a PASSIVE one.
 *
 * rollInterjectors is a separate step, always applied after select() : every other PRESENT or
 * PASSIVE Npc gets an independent roll to react briefly anyway ; a total chance is shared among them
 * (double weight for PRESENT), so it shrinks as the number of candidates grows.
 * An ABSENT Npc never speaks.
 */
public final class SpeakerSelector {

    private SpeakerSelector() {}

    private static final int MENTION_PREFIX_LENGTH = 5;
    private static final int PRESENT_WEIGHT = 2;
    private static final int PASSIVE_WEIGHT = 1;
    private static final double MAX_INTERJECTION_CHANCE_PER_NPC = 0.6;

    /**
     * activity must leave somebody in the scene — ChatSession.cycleActivity and the scenario's own
     * validation guard against emptying it. previousRoundReplies are the LLM turns of the round before
     * this message (empty on the very first exchange) — see ChatServiceImpl for how they are derived
     * from session.turns().
     */
    public static List<Npc> select(Cast cast, CastActivity activity, String playerMessage,
                                    List<ChatTurn> previousRoundReplies, Random random) {
        List<Npc> inScene = inScene(cast, activity.inSceneIds());
        List<Npc> speakers = new ArrayList<>(mentionedAmong(inScene, activity, playerMessage, previousRoundReplies));
        for (Npc npc : inScene) {
            if (activity.levelOf(npc.id()) == NpcActivity.ACTIVE && !speakers.contains(npc)) speakers.add(npc);
        }
        if (!speakers.isEmpty()) return List.copyOf(speakers);

        Set<String> previousSpeakers = speakerIds(previousRoundReplies);
        if (previousSpeakers.size() == 1) {
            String onlyId = previousSpeakers.iterator().next();
            Optional<Npc> continuing = inScene.stream().filter(n -> n.id().equals(onlyId)).findFirst();
            if (continuing.isPresent()) return List.of(continuing.get());
        }
        return List.of(randomPick(inScene, activity, random));
    }

    /**
     * The Npcs who open a story or act (see NpcOpening), in the order the author wrote them : the
     * named ones that are present (the first opens, the others follow), otherwise one present Npc
     * picked at random (a named Npc muted by the player is not dragged back into the scene). Unlike
     * select(), mentions, continuation and activity levels play no role : the author's marker is the
     * only input. presentIds must not be empty.
     */
    public static List<Npc> openingSpeakers(Cast cast, Set<String> presentIds, NpcOpening opening, Random random) {
        List<Npc> named = ActivityResolver.openingNpcs(cast, opening).stream()
            .filter(n -> presentIds.contains(n.id()))
            .toList();
        if (!named.isEmpty()) return named;
        List<Npc> present = inScene(cast, presentIds);
        return List.of(present.get(random.nextInt(present.size())));
    }

    /**
     * Every PRESENT or PASSIVE Npc other than the primary speaker(s) gets an independent roll to
     * react briefly anyway. totalChance is the chance of the whole group : it is shared between the
     * candidates in proportion to their weight (a PRESENT counts twice a PASSIVE), so the expected
     * number of reactions per round stays near totalChance whatever the number of Npcs — the more
     * candidates, the smaller each chance. One Npc never exceeds MAX_INTERJECTION_CHANCE_PER_NPC,
     * which means a lone candidate does not react every single round. A totalChance above 1 raises
     * that cap in proportion (a very large value makes everybody react every round).
     * Applied regardless of how primarySpeakers was resolved. Result order is randomized (not
     * id-sorted) : nothing about who happens to react unprompted should read as more "senior" than
     * another.
     */
    public static List<Npc> rollInterjectors(Cast cast, CastActivity activity, List<Npc> primarySpeakers,
                                              double totalChance, Random random) {
        Set<String> primaryIds = primarySpeakers.stream().map(Npc::id).collect(Collectors.toSet());
        List<Npc> candidates = inScene(cast, activity.inSceneIds()).stream()
            .filter(npc -> !primaryIds.contains(npc.id()))
            .filter(npc -> activity.levelOf(npc.id()) != NpcActivity.ACTIVE)
            .toList();
        int totalWeight = candidates.stream().mapToInt(npc -> interjectionWeight(activity.levelOf(npc.id()))).sum();
        double maxPerNpc = MAX_INTERJECTION_CHANCE_PER_NPC * Math.max(1.0, totalChance);

        List<Npc> interjectors = new ArrayList<>();
        for (Npc candidate : candidates) {
            double share = totalChance * interjectionWeight(activity.levelOf(candidate.id())) / totalWeight;
            if (random.nextDouble() < Math.min(maxPerNpc, share)) interjectors.add(candidate);
        }
        Collections.shuffle(interjectors, random);
        return List.copyOf(interjectors);
    }

    private static int interjectionWeight(NpcActivity level) {
        return level == NpcActivity.PRESENT ? PRESENT_WEIGHT : PASSIVE_WEIGHT;
    }

    private static Npc randomPick(List<Npc> inScene, CastActivity activity, Random random) {
        List<Npc> pool = new ArrayList<>();
        int total = 0;
        for (Npc npc : inScene) {
            NpcActivity level = activity.levelOf(npc.id());
            if (level == NpcActivity.PRESENT) { pool.add(npc); total += PRESENT_WEIGHT; }
            else if (level == NpcActivity.PASSIVE) { pool.add(npc); total += PASSIVE_WEIGHT; }
        }
        if (pool.isEmpty()) return inScene.get(random.nextInt(inScene.size()));
        int roll = random.nextInt(total);
        for (Npc npc : pool) {
            roll -= activity.levelOf(npc.id()) == NpcActivity.PRESENT ? PRESENT_WEIGHT : PASSIVE_WEIGHT;
            if (roll < 0) return npc;
        }
        return pool.get(pool.size() - 1);
    }

    private static Set<String> speakerIds(List<ChatTurn> replies) {
        Set<String> ids = new LinkedHashSet<>();
        for (ChatTurn reply : replies) if (reply.npcId() != null) ids.add(reply.npcId());
        return ids;
    }

    private static List<Npc> inScene(Cast cast, Set<String> presentIds) {
        List<Npc> inScene = cast.npcs().stream()
            .filter(n -> presentIds.contains(n.id()))
            .sorted(Comparator.comparing(Npc::id))
            .toList();
        if (inScene.isEmpty()) {
            throw new IllegalStateException("Aucun personnage present pour repondre.");
        }
        return inScene;
    }

    /**
     * ACTIVE or PRESENT Npcs named in the player's message (by position), then those named only in a
     * reply of the previous round (by reply order, then position) — never by their own reply.
     */
    private static List<Npc> mentionedAmong(List<Npc> inScene, CastActivity activity, String playerMessage,
                                             List<ChatTurn> previousRoundReplies) {
        List<Npc> candidates = inScene.stream()
            .filter(n -> activity.levelOf(n.id()) == NpcActivity.ACTIVE || activity.levelOf(n.id()) == NpcActivity.PRESENT)
            .toList();
        List<Npc> mentioned = new ArrayList<>(inOrderOfMention(candidates, playerMessage));
        for (ChatTurn reply : previousRoundReplies) {
            List<Npc> others = candidates.stream().filter(n -> !n.id().equals(reply.npcId())).toList();
            for (Npc npc : inOrderOfMention(others, reply.text())) {
                if (!mentioned.contains(npc)) mentioned.add(npc);
            }
        }
        return mentioned;
    }

    /** The candidates mentioned in text, ordered by where their name FIRST appears in it. */
    private static List<Npc> inOrderOfMention(List<Npc> candidates, String text) {
        record Mention(Npc npc, int position) {}
        return candidates.stream()
            .map(n -> new Mention(n, firstMentionPosition(text, n.name())))
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
