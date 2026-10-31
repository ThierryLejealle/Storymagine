package storymagine.chat.coeur.domaine.session;

import org.junit.jupiter.api.Test;
import storymagine.chat.coeur.domaine.activity.ActivitySource;
import storymagine.chat.coeur.domaine.activity.CastActivity;
import storymagine.chat.coeur.domaine.activity.NpcActivity;
import storymagine.chat.coeur.domaine.activity.NpcActivityState;
import storymagine.chat.coeur.domaine.scenario.Cast;
import storymagine.chat.coeur.domaine.scenario.Npc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpeakerSelectorTest {

    private static final Npc ELENA  = new Npc("elena", "Elena Voss", "A sharp-tongued merchant.", "");
    private static final Npc MARCUS = new Npc("marcus", "Marcus", "A retired soldier.", "");
    private static final Npc CLARA  = new Npc("clara", "Clara", "A quiet bard.", "");
    private static final Cast CAST  = new Cast(List.of(ELENA, MARCUS));
    private static final Cast THREE = new Cast(List.of(ELENA, MARCUS, CLARA));
    private static final List<ChatTurn> NOBODY_SPOKE_BEFORE = List.of();

    /** Everybody present (the default), except the levels given. */
    private static CastActivity activity(Cast cast, Object... idsAndLevels) {
        CastActivity activity = CastActivity.defaults(cast);
        for (int i = 0; i < idsAndLevels.length; i += 2) {
            activity = activity.with((String) idsAndLevels[i],
                new NpcActivityState((NpcActivity) idsAndLevels[i + 1], ActivitySource.EXPLICIT));
        }
        return activity;
    }

    private static ChatTurn reply(String npcId, String text) {
        return new ChatTurn(ChatTurn.Speaker.LLM, text, null, npcId);
    }

    // ── mentions in the player's message ─────────────────────────────────────

    @Test
    void selectsOnlyTheNpcMentionedByFirstNameOfATwoWordName() {
        List<Npc> chosen = SpeakerSelector.select(CAST, activity(CAST), "Elena, what do you make of this?",
            NOBODY_SPOKE_BEFORE, new Random());

        assertEquals(List.of(ELENA), chosen);
    }

    @Test
    void mentionMatchesAnySingleWordOfAMultiWordName() {
        List<Npc> chosen = SpeakerSelector.select(CAST, activity(CAST), "I've heard of a Voss before.",
            NOBODY_SPOKE_BEFORE, new Random());

        assertEquals(List.of(ELENA), chosen);
    }

    @Test
    void mentionIsForgivingOfAccentsCaseAndShortenedNames() {
        Npc celeste = new Npc("celeste", "Céleste", "A paladin.", "");
        Cast cast = new Cast(List.of(celeste));

        // seuls les 5 premiers caracteres (normalises) du nom doivent matcher
        List<Npc> chosen = SpeakerSelector.select(cast, activity(cast), "CELESTINE, tu es la ?",
            NOBODY_SPOKE_BEFORE, new Random());

        assertEquals(List.of(celeste), chosen);
    }

    @Test
    void mentionRequiresAtLeastTheFirstFiveNormalizedCharacters() {
        // "Marc" ne fait que 4 caracteres : ne doit pas declencher "Marcus" — repli sur un PNJ au hasard.
        List<Npc> chosen = SpeakerSelector.select(CAST, activity(CAST), "Marc left a note here.",
            NOBODY_SPOKE_BEFORE, new Random());

        assertEquals(1, chosen.size());
    }

    @Test
    void bothMentionedNpcsAnswerInTheOrderTheyAppearInTheMessage() {
        List<Npc> chosen = SpeakerSelector.select(CAST, activity(CAST), "Marcus, est-ce qu'Elena va bien ?",
            NOBODY_SPOKE_BEFORE, new Random());

        assertEquals(List.of(MARCUS, ELENA), chosen, "l'ordre d'apparition dans le message prime sur l'id");
    }

    @Test
    void anAbsentNpcIsNeverEligibleEvenIfMentioned() {
        List<Npc> chosen = SpeakerSelector.select(CAST, activity(CAST, "elena", NpcActivity.ABSENT),
            "Elena, are you there?", NOBODY_SPOKE_BEFORE, new Random());

        assertEquals(List.of(MARCUS), chosen);
    }

    @Test
    void aPassiveNpcDoesNotAnswerJustBecauseItIsNamed() {
        // passif : le nommer ne suffit pas, seul le tirage au sort ou l'interjection peut le faire parler.
        List<Npc> chosen = SpeakerSelector.select(THREE,
            activity(THREE, "elena", NpcActivity.PASSIVE, "clara", NpcActivity.ACTIVE),
            "Elena, are you there?", NOBODY_SPOKE_BEFORE, new Random());

        assertEquals(List.of(CLARA), chosen, "seule l'active parle : elena (passive) est nommee mais ne repond pas");
    }

    // ── mentions in the previous round ───────────────────────────────────────

    @Test
    void aPresentNpcMentionedByAnotherNpcInThePreviousRoundMayAnswer() {
        // marcus a parle d'Elena au tour precedent : Elena a le droit de repondre, meme si le joueur ne la nomme pas.
        List<Npc> chosen = SpeakerSelector.select(CAST, activity(CAST), "Et après ?",
            List.of(reply("marcus", "Elena knows more than she says.")), new Random());

        assertEquals(List.of(ELENA), chosen);
    }

    @Test
    void anNpcNeverAnswersBecauseItMentionedItself() {
        List<Npc> chosen = SpeakerSelector.select(CAST, activity(CAST), "Et après ?",
            List.of(reply("marcus", "Marcus never forgets.")), new Random());

        assertEquals(List.of(MARCUS), chosen, "pas de mention : c'est la continuite du seul locuteur precedent");
    }

    @Test
    void thePlayersMentionsComeBeforeThoseOfTheNpcReplies() {
        List<Npc> chosen = SpeakerSelector.select(THREE, activity(THREE), "Clara, et toi ?",
            List.of(reply("marcus", "Elena, tell them.")), new Random());

        assertEquals(List.of(CLARA, ELENA), chosen);
    }

    // ── active ───────────────────────────────────────────────────────────────

    @Test
    void anActiveNpcAnswersEvenWhenSomeoneElseIsNamed() {
        List<Npc> chosen = SpeakerSelector.select(THREE, activity(THREE, "clara", NpcActivity.ACTIVE),
            "Marcus, à toi.", NOBODY_SPOKE_BEFORE, new Random());

        assertEquals(List.of(MARCUS, CLARA), chosen, "le nomme d'abord, puis les actifs");
    }

    @Test
    void activeNpcsAnswerInIdOrderWhenNobodyIsNamed() {
        List<Npc> chosen = SpeakerSelector.select(THREE,
            activity(THREE, "marcus", NpcActivity.ACTIVE, "clara", NpcActivity.ACTIVE),
            "Hello there.", NOBODY_SPOKE_BEFORE, new Random());

        assertEquals(List.of(CLARA, MARCUS), chosen);
    }

    @Test
    void anActiveNpcSpeakingAloneSuppressesTheRandomFallback() {
        for (int seed = 0; seed < 20; seed++) {
            List<Npc> chosen = SpeakerSelector.select(THREE, activity(THREE, "clara", NpcActivity.ACTIVE),
                "Hello there.", NOBODY_SPOKE_BEFORE, new Random(seed));
            assertEquals(List.of(CLARA), chosen, "seed=" + seed);
        }
    }

    // ── continuation ─────────────────────────────────────────────────────────

    @Test
    void continuesWithTheSingleNpcWhoSpokeInThePreviousRoundWhenNobodyIsMentioned() {
        List<Npc> chosen = SpeakerSelector.select(CAST, activity(CAST), "Et après ?",
            List.of(reply("marcus", "Nothing.")), new Random());

        assertEquals(List.of(MARCUS), chosen);
    }

    @Test
    void mentionStillTakesPriorityOverContinuation() {
        List<Npc> chosen = SpeakerSelector.select(CAST, activity(CAST), "Elena, à toi.",
            List.of(reply("marcus", "Nothing.")), new Random());

        assertEquals(List.of(ELENA), chosen);
    }

    @Test
    void continuationIsIgnoredIfThatNpcIsNoLongerInTheScene() {
        List<Npc> chosen = SpeakerSelector.select(CAST, activity(CAST, "marcus", NpcActivity.ABSENT), "Et après ?",
            List.of(reply("marcus", "Nothing.")), new Random());

        assertEquals(List.of(ELENA), chosen);
    }

    @Test
    void fallsBackToRandomWhenThePreviousRoundHadSeveralSpeakers() {
        List<Npc> chosen = SpeakerSelector.select(CAST, activity(CAST), "Et après ?",
            List.of(reply("elena", "Hm."), reply("marcus", "Hm.")), new Random(42));

        assertEquals(1, chosen.size());
    }

    // ── random pick ──────────────────────────────────────────────────────────

    @Test
    void throwsWhenNobodyIsInTheScene() {
        CastActivity nobody = activity(CAST, "elena", NpcActivity.ABSENT, "marcus", NpcActivity.ABSENT);
        assertThrows(IllegalStateException.class,
            () -> SpeakerSelector.select(CAST, nobody, "Hello?", NOBODY_SPOKE_BEFORE, new Random()));
    }

    @Test
    void aPresentNpcIsPickedTwiceAsOftenAsAPassiveOne() {
        // Elena present (poids 2), Marcus passif (poids 1) : sur beaucoup de tirages, environ deux tiers pour Elena.
        CastActivity activity = activity(CAST, "marcus", NpcActivity.PASSIVE);
        Random random = new Random(7);
        Map<String, Integer> counts = new HashMap<>();
        for (int i = 0; i < 3000; i++) {
            String id = SpeakerSelector.select(CAST, activity, "Hello there.", NOBODY_SPOKE_BEFORE, random).get(0).id();
            counts.merge(id, 1, Integer::sum);
        }

        double elenaShare = counts.getOrDefault("elena", 0) / 3000.0;
        assertTrue(elenaShare > 0.62 && elenaShare < 0.71, "part d'Elena = " + elenaShare);
    }

    @Test
    void aPassiveNpcCanBePickedAtRandomWhenNobodyElseIsEligible() {
        List<Npc> chosen = SpeakerSelector.select(CAST, activity(CAST, "elena", NpcActivity.ABSENT, "marcus", NpcActivity.PASSIVE),
            "Hello there.", NOBODY_SPOKE_BEFORE, new Random());

        assertEquals(List.of(MARCUS), chosen);
    }

    // ── rollInterjectors ─────────────────────────────────────────────────────

    /** Share of the rolls in which the given Npc interjects, over many rolls. */
    private static double interjectionRate(Cast cast, CastActivity activity, List<Npc> primary, double totalChance, Npc npc) {
        Random random = new Random(7);
        int hits = 0;
        for (int i = 0; i < 4000; i++) {
            if (SpeakerSelector.rollInterjectors(cast, activity, primary, totalChance, random).contains(npc)) hits++;
        }
        return hits / 4000.0;
    }

    @Test
    void rollInterjectorsAppliesEvenWhenSeveralNpcsAreAlreadyPrimary() {
        assertTrue(interjectionRate(THREE, activity(THREE), List.of(ELENA, MARCUS), 1.0, CLARA) > 0.5);
    }

    @Test
    void rollInterjectorsNeverIncludesAPrimarySpeaker() {
        assertEquals(0.0, interjectionRate(THREE, activity(THREE), List.of(ELENA), 1.0, ELENA));
        assertTrue(interjectionRate(THREE, activity(THREE), List.of(ELENA), 1.0, MARCUS) > 0.2);
    }

    @Test
    void rollInterjectorsSkipsAbsentNpcs() {
        CastActivity activity = activity(THREE, "clara", NpcActivity.ABSENT);

        assertEquals(0.0, interjectionRate(THREE, activity, List.of(ELENA), 1.0, CLARA));
        assertTrue(interjectionRate(THREE, activity, List.of(ELENA), 1.0, MARCUS) > 0.5);
    }

    @Test
    void zeroChanceNeverInterjects() {
        List<Npc> interjectors = SpeakerSelector.rollInterjectors(THREE, activity(THREE), List.of(ELENA), 0.0, new Random());

        assertEquals(List.of(), interjectors);
    }

    @Test
    void aPresentNpcInterjectsWithADoubleChance() {
        // chance totale 0.75 partagee en poids 2 (Clara, presente) + 1 (Marcus, passif) : 0.5 et 0.25.
        // Le plafond par PNJ (0.6) n'est pas atteint.
        CastActivity activity = activity(THREE, "marcus", NpcActivity.PASSIVE);

        double clara = interjectionRate(THREE, activity, List.of(ELENA), 0.75, CLARA);
        double marcus = interjectionRate(THREE, activity, List.of(ELENA), 0.75, MARCUS);

        assertEquals(0.5, clara, 0.04, "present (Clara)");
        assertEquals(0.25, marcus, 0.04, "passif (Marcus)");
    }

    @Test
    void theTotalChanceIsSharedSoEachNpcReactsLessWhenThereAreMoreCandidates() {
        // 4 presents candidats, chance totale 1.0 : 25 % chacun, soit environ une reaction par tour au total.
        Npc a = new Npc("a", "Ana", "", ""), b = new Npc("b", "Bea", "", ""), c = new Npc("c", "Cleo", "", "");
        Npc d = new Npc("d", "Dora", "", ""), e = new Npc("e", "Eve", "", "");
        Cast five = new Cast(List.of(a, b, c, d, e));
        CastActivity activity = activity(five, "a", NpcActivity.ACTIVE);

        Random random = new Random(11);
        int reactions = 0;
        for (int i = 0; i < 4000; i++) {
            reactions += SpeakerSelector.rollInterjectors(five, activity, List.of(a), 1.0, random).size();
        }

        assertEquals(1.0, reactions / 4000.0, 0.06);
        assertEquals(0.25, interjectionRate(five, activity, List.of(a), 1.0, b), 0.04);
    }

    @Test
    void aLoneCandidateNeverExceedsTheCapPerNpc() {
        CastActivity activity = activity(THREE, "clara", NpcActivity.ABSENT);

        assertEquals(0.6, interjectionRate(THREE, activity, List.of(ELENA), 1.0, MARCUS), 0.04);
    }
}
