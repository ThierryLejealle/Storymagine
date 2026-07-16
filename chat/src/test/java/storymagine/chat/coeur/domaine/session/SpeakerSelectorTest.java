package storymagine.chat.coeur.domaine.session;

import org.junit.jupiter.api.Test;
import storymagine.chat.coeur.domaine.scenario.Cast;
import storymagine.chat.coeur.domaine.scenario.Npc;

import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpeakerSelectorTest {

    private static final Npc ELENA  = new Npc("elena", "Elena Voss", "A sharp-tongued merchant.", "");
    private static final Npc MARCUS = new Npc("marcus", "Marcus", "A retired soldier.", "");
    private static final Cast CAST  = new Cast(List.of(ELENA, MARCUS));
    private static final Set<String> BOTH_PRESENT = Set.of("elena", "marcus");

    @Test
    void selectsOnlyTheNpcMentionedByFirstNameOfATwoWordName() {
        List<Npc> chosen = SpeakerSelector.select(CAST, BOTH_PRESENT, BOTH_PRESENT, "Elena, what do you make of this?", new Random());

        assertEquals(List.of(ELENA), chosen);
    }

    @Test
    void mentionMatchesAnySingleWordOfAMultiWordName() {
        List<Npc> chosen = SpeakerSelector.select(CAST, BOTH_PRESENT, BOTH_PRESENT, "I've heard of a Voss before.", new Random());

        assertEquals(List.of(ELENA), chosen);
    }

    @Test
    void mentionRequiresAWholeWordNotASubstring() {
        // "Marc" ne doit pas declencher "Marcus" — mot entier uniquement ; personne mentionne
        // (ni elena ni marcus) : repli en aleatoire, les deux sont eligibles ici.
        List<Npc> chosen = SpeakerSelector.select(CAST, BOTH_PRESENT, BOTH_PRESENT, "Marc left a note here.", new Random());

        assertEquals(2, chosen.size());
    }

    @Test
    void bothMentionedNpcsAnswerInStableAlphabeticalOrder() {
        List<Npc> chosen = SpeakerSelector.select(CAST, BOTH_PRESENT, BOTH_PRESENT, "Elena and Marcus both stare at me.", new Random());

        assertEquals(List.of(ELENA, MARCUS), chosen);
    }

    @Test
    void fallsBackToTwoRandomPresentNpcsWhenNobodyIsMentioned() {
        List<Npc> chosen = SpeakerSelector.select(CAST, BOTH_PRESENT, BOTH_PRESENT, "Hello there.", new Random(42));

        assertEquals(2, chosen.size());
        assertTrue(chosen.containsAll(List.of(ELENA, MARCUS)));
    }

    @Test
    void fallsBackToTheSingleAvailableNpcWhenOnlyOneIsPresentAndNobodyIsMentioned() {
        List<Npc> chosen = SpeakerSelector.select(CAST, Set.of("marcus"), Set.of("marcus"), "Hello there.", new Random());

        assertEquals(List.of(MARCUS), chosen);
    }

    @Test
    void absentNpcsAreNeverEligibleEvenIfMentioned() {
        List<Npc> chosen = SpeakerSelector.select(CAST, Set.of("marcus"), Set.of("marcus"), "Elena, are you there?", new Random());

        assertEquals(List.of(MARCUS), chosen, "elena est absente : sa mention est ignoree, seul marcus est eligible");
    }

    @Test
    void throwsWhenNoNpcIsPresent() {
        assertThrows(IllegalStateException.class,
            () -> SpeakerSelector.select(CAST, Set.of(), Set.of(), "Hello?", new Random()));
    }

    @Test
    void randomFallbackNeverPicksAnNpcThatOptedOutOfInterjecting() {
        // marcus a decoche son bouton interjection : personne ne le nomme, il ne doit jamais etre
        // pioche par le repli aleatoire — meme promesse que rollInterjectors (tooltip "ne reagit
        // que si vise par son nom").
        for (int seed = 0; seed < 20; seed++) {
            List<Npc> chosen = SpeakerSelector.select(CAST, BOTH_PRESENT, Set.of("elena"), "Hello there.", new Random(seed));
            assertEquals(List.of(ELENA), chosen, "seed=" + seed);
        }
    }

    @Test
    void randomFallbackFallsBackToEveryPresentNpcWhenNoneAreInterjectionEligible() {
        // les deux ont decoche leur bouton : quelqu'un doit quand meme repondre au joueur.
        List<Npc> chosen = SpeakerSelector.select(CAST, BOTH_PRESENT, Set.of(), "Hello there.", new Random(42));

        assertEquals(2, chosen.size());
        assertTrue(chosen.containsAll(List.of(ELENA, MARCUS)));
    }

    // ── rollInterjectors ─────────────────────────────────────────────────────

    private static final Npc CLARA = new Npc("clara", "Clara", "A quiet bard.", "");
    private static final Cast THREE_NPC_CAST = new Cast(List.of(ELENA, MARCUS, CLARA));
    private static final Set<String> ALL_THREE_PRESENT = Set.of("elena", "marcus", "clara");

    @Test
    void rollInterjectorsIsEmptyWhenMultipleNpcsWereMentioned() {
        List<Npc> primary = List.of(ELENA, MARCUS);

        List<Npc> interjectors = SpeakerSelector.rollInterjectors(THREE_NPC_CAST, ALL_THREE_PRESENT,
            ALL_THREE_PRESENT, primary, "Elena and Marcus, look!", 1.0, new Random());

        assertEquals(List.of(), interjectors);
    }

    @Test
    void rollInterjectorsIsEmptyWhenThePrimaryCameFromTheRandomFallbackNotAMention() {
        List<Npc> primary = SpeakerSelector.select(CAST, BOTH_PRESENT, BOTH_PRESENT, "Hello there.", new Random(1));

        List<Npc> interjectors = SpeakerSelector.rollInterjectors(CAST, BOTH_PRESENT, BOTH_PRESENT, primary,
            "Hello there.", 1.0, new Random());

        assertEquals(List.of(), interjectors);
    }

    @Test
    void rollInterjectorsNeverIncludesTheTargetedNpcEvenAtFullChance() {
        List<Npc> interjectors = SpeakerSelector.rollInterjectors(THREE_NPC_CAST, ALL_THREE_PRESENT,
            ALL_THREE_PRESENT, List.of(ELENA), "Elena, look out!", 1.0, new Random());

        // ordre stable par id alphabetique (meme convention que select()) : "clara" < "marcus"
        assertEquals(List.of(CLARA, MARCUS), interjectors);
    }

    @Test
    void rollInterjectorsSkipsNpcsNotOptedIn() {
        List<Npc> interjectors = SpeakerSelector.rollInterjectors(THREE_NPC_CAST, ALL_THREE_PRESENT,
            Set.of("marcus"), List.of(ELENA), "Elena, look out!", 1.0, new Random());

        assertEquals(List.of(MARCUS), interjectors, "clara n'est pas eligible (pas dans interjectingIds)");
    }

    @Test
    void rollInterjectorsSkipsAbsentNpcsEvenIfEligible() {
        List<Npc> interjectors = SpeakerSelector.rollInterjectors(THREE_NPC_CAST, Set.of("elena", "marcus"),
            ALL_THREE_PRESENT, List.of(ELENA), "Elena, look out!", 1.0, new Random());

        assertEquals(List.of(MARCUS), interjectors, "clara est absente, jamais candidate");
    }

    @Test
    void zeroChanceNeverInterjectsEvenWhenEligible() {
        List<Npc> interjectors = SpeakerSelector.rollInterjectors(THREE_NPC_CAST, ALL_THREE_PRESENT,
            ALL_THREE_PRESENT, List.of(ELENA), "Elena, look out!", 0.0, new Random());

        assertEquals(List.of(), interjectors);
    }
}
