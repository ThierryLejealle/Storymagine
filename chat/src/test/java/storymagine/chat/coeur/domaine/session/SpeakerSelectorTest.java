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
    private static final Set<String> NOBODY_SPOKE_BEFORE = Set.of();

    // ── select() : mentions ──────────────────────────────────────────────────

    @Test
    void selectsOnlyTheNpcMentionedByFirstNameOfATwoWordName() {
        List<Npc> chosen = SpeakerSelector.select(CAST, BOTH_PRESENT, BOTH_PRESENT,
            "Elena, what do you make of this?", NOBODY_SPOKE_BEFORE, new Random());

        assertEquals(List.of(ELENA), chosen);
    }

    @Test
    void mentionMatchesAnySingleWordOfAMultiWordName() {
        List<Npc> chosen = SpeakerSelector.select(CAST, BOTH_PRESENT, BOTH_PRESENT,
            "I've heard of a Voss before.", NOBODY_SPOKE_BEFORE, new Random());

        assertEquals(List.of(ELENA), chosen);
    }

    @Test
    void mentionIsForgivingOfAccentsCaseAndShortenedNames() {
        Npc celeste = new Npc("celeste", "Céleste", "A paladin.", "");
        Cast cast = new Cast(List.of(celeste));
        Set<String> present = Set.of("celeste");

        // seuls les 5 premiers caracteres (normalises) du nom doivent matcher — accents/majuscules
        // ignores, et un nom raccourci ou legerement fautif suffit.
        List<Npc> chosen = SpeakerSelector.select(cast, present, present,
            "CELESTINE, tu es la ?", NOBODY_SPOKE_BEFORE, new Random());

        assertEquals(List.of(celeste), chosen);
    }

    @Test
    void mentionRequiresAtLeastTheFirstFiveNormalizedCharacters() {
        // "Marc" ne fait que 4 caracteres : ne doit pas declencher "Marcus" (prefixe "marcu",
        // 5 caracteres) — repli sur un seul PNJ au hasard puisque personne n'est reellement vise.
        List<Npc> chosen = SpeakerSelector.select(CAST, BOTH_PRESENT, BOTH_PRESENT,
            "Marc left a note here.", NOBODY_SPOKE_BEFORE, new Random());

        assertEquals(1, chosen.size());
    }

    @Test
    void bothMentionedNpcsAnswerInTheOrderTheyAppearInTheMessage() {
        List<Npc> chosen = SpeakerSelector.select(CAST, BOTH_PRESENT, BOTH_PRESENT,
            "Elena and Marcus both stare at me.", NOBODY_SPOKE_BEFORE, new Random());

        assertEquals(List.of(ELENA, MARCUS), chosen);
    }

    @Test
    void theFirstNpcNamedInTheMessageAnswersFirstEvenIfItComesAfterAlphabetically() {
        // Marcus est nomme en premier dans le texte, alors que "elena" < "marcus" alphabetiquement
        // — c'est l'ORDRE D'APPARITION dans le message qui doit primer, pas l'id.
        List<Npc> chosen = SpeakerSelector.select(CAST, BOTH_PRESENT, BOTH_PRESENT,
            "Marcus, est-ce qu'Elena va bien ?", NOBODY_SPOKE_BEFORE, new Random());

        assertEquals(List.of(MARCUS, ELENA), chosen);
    }

    @Test
    void absentNpcsAreNeverEligibleEvenIfMentioned() {
        List<Npc> chosen = SpeakerSelector.select(CAST, Set.of("marcus"), Set.of("marcus"),
            "Elena, are you there?", NOBODY_SPOKE_BEFORE, new Random());

        assertEquals(List.of(MARCUS), chosen, "elena est absente : sa mention est ignoree, seul marcus est eligible");
    }

    // ── select() : continuation (aucune mention) ────────────────────────────

    @Test
    void continuesWithTheSingleNpcWhoSpokeInThePreviousRoundWhenNobodyIsMentioned() {
        List<Npc> chosen = SpeakerSelector.select(CAST, BOTH_PRESENT, BOTH_PRESENT,
            "Et après ?", Set.of("marcus"), new Random());

        assertEquals(List.of(MARCUS), chosen);
    }

    @Test
    void mentionStillTakesPriorityOverContinuation() {
        // marcus a parle au tour precedent, mais le joueur vise explicitement elena cette fois.
        List<Npc> chosen = SpeakerSelector.select(CAST, BOTH_PRESENT, BOTH_PRESENT,
            "Elena, à toi.", Set.of("marcus"), new Random());

        assertEquals(List.of(ELENA), chosen);
    }

    @Test
    void continuationIsIgnoredIfThatNpcIsNoLongerPresent() {
        // marcus a parle au tour precedent mais n'est plus present (mute depuis) : repli aleatoire
        // sur les PNJ presents, pas d'exception, pas de tentative de le faire parler quand meme.
        List<Npc> chosen = SpeakerSelector.select(CAST, Set.of("elena"), Set.of("elena"),
            "Et après ?", Set.of("marcus"), new Random());

        assertEquals(List.of(ELENA), chosen);
    }

    @Test
    void fallsBackToRandomWhenTheePreviousRoundHadSeveralSpeakers() {
        // pas UN seul PNJ qui a parle avant (deux ont parle) : pas de continuation possible, repli
        // aleatoire classique.
        List<Npc> chosen = SpeakerSelector.select(CAST, BOTH_PRESENT, BOTH_PRESENT,
            "Et après ?", Set.of("elena", "marcus"), new Random(42));

        assertEquals(1, chosen.size());
    }

    @Test
    void fallsBackToRandomOnTheVeryFirstExchange() {
        List<Npc> chosen = SpeakerSelector.select(CAST, BOTH_PRESENT, BOTH_PRESENT,
            "Hello there.", NOBODY_SPOKE_BEFORE, new Random(42));

        assertEquals(1, chosen.size());
    }

    // ── select() : repli aleatoire ───────────────────────────────────────────

    @Test
    void fallsBackToTheSingleAvailableNpcWhenOnlyOneIsPresentAndNobodyIsMentioned() {
        List<Npc> chosen = SpeakerSelector.select(CAST, Set.of("marcus"), Set.of("marcus"),
            "Hello there.", NOBODY_SPOKE_BEFORE, new Random());

        assertEquals(List.of(MARCUS), chosen);
    }

    @Test
    void throwsWhenNoNpcIsPresent() {
        assertThrows(IllegalStateException.class,
            () -> SpeakerSelector.select(CAST, Set.of(), Set.of(), "Hello?", NOBODY_SPOKE_BEFORE, new Random()));
    }

    @Test
    void randomFallbackNeverPicksAnNpcThatOptedOutOfInterjecting() {
        // marcus a decoche son bouton interjection : personne ne le nomme, il ne doit jamais etre
        // pioche par le repli aleatoire — meme promesse que rollInterjectors (tooltip "ne reagit
        // que si vise par son nom").
        for (int seed = 0; seed < 20; seed++) {
            List<Npc> chosen = SpeakerSelector.select(CAST, BOTH_PRESENT, Set.of("elena"),
                "Hello there.", NOBODY_SPOKE_BEFORE, new Random(seed));
            assertEquals(List.of(ELENA), chosen, "seed=" + seed);
        }
    }

    @Test
    void randomFallbackFallsBackToEveryPresentNpcWhenNoneAreInterjectionEligible() {
        // les deux ont decoche leur bouton : quelqu'un doit quand meme repondre au joueur.
        List<Npc> chosen = SpeakerSelector.select(CAST, BOTH_PRESENT, Set.of(),
            "Hello there.", NOBODY_SPOKE_BEFORE, new Random(42));

        assertEquals(1, chosen.size());
        assertTrue(BOTH_PRESENT.contains(chosen.get(0).id()));
    }

    // ── rollInterjectors ─────────────────────────────────────────────────────

    private static final Npc CLARA = new Npc("clara", "Clara", "A quiet bard.", "");
    private static final Cast THREE_NPC_CAST = new Cast(List.of(ELENA, MARCUS, CLARA));
    private static final Set<String> ALL_THREE_PRESENT = Set.of("elena", "marcus", "clara");

    @Test
    void rollInterjectorsAppliesEvenWhenSeveralNpcsAreAlreadyPrimary() {
        // elena ET marcus sont deja principaux (mention multiple) : clara doit quand meme pouvoir
        // reagir — plus de restriction "un seul primaire" depuis la refonte.
        List<Npc> primary = List.of(ELENA, MARCUS);

        List<Npc> interjectors = SpeakerSelector.rollInterjectors(THREE_NPC_CAST, ALL_THREE_PRESENT,
            ALL_THREE_PRESENT, primary, 1.0, new Random());

        assertEquals(List.of(CLARA), interjectors);
    }

    @Test
    void rollInterjectorsAppliesEvenWhenThePrimaryCameFromARandomOrContinuationPick() {
        // meme quand le PNJ principal ne vient pas d'une mention explicite, les autres doivent
        // pouvoir reagir — c'est tout le sens du changement demande.
        List<Npc> interjectors = SpeakerSelector.rollInterjectors(THREE_NPC_CAST, ALL_THREE_PRESENT,
            ALL_THREE_PRESENT, List.of(ELENA), 1.0, new Random());

        assertEquals(Set.of(CLARA, MARCUS), Set.copyOf(interjectors));
    }

    @Test
    void rollInterjectorsNeverIncludesAPrimarySpeaker() {
        List<Npc> interjectors = SpeakerSelector.rollInterjectors(THREE_NPC_CAST, ALL_THREE_PRESENT,
            ALL_THREE_PRESENT, List.of(ELENA), 1.0, new Random());

        assertEquals(Set.of(CLARA, MARCUS), Set.copyOf(interjectors));
        assertTrue(interjectors.stream().noneMatch(n -> n.id().equals("elena")));
    }

    @Test
    void rollInterjectorsSkipsNpcsNotOptedIn() {
        List<Npc> interjectors = SpeakerSelector.rollInterjectors(THREE_NPC_CAST, ALL_THREE_PRESENT,
            Set.of("marcus"), List.of(ELENA), 1.0, new Random());

        assertEquals(List.of(MARCUS), interjectors, "clara n'est pas eligible (pas dans interjectingIds)");
    }

    @Test
    void rollInterjectorsSkipsAbsentNpcsEvenIfEligible() {
        List<Npc> interjectors = SpeakerSelector.rollInterjectors(THREE_NPC_CAST, Set.of("elena", "marcus"),
            ALL_THREE_PRESENT, List.of(ELENA), 1.0, new Random());

        assertEquals(List.of(MARCUS), interjectors, "clara est absente, jamais candidate");
    }

    @Test
    void zeroChanceNeverInterjectsEvenWhenEligible() {
        List<Npc> interjectors = SpeakerSelector.rollInterjectors(THREE_NPC_CAST, ALL_THREE_PRESENT,
            ALL_THREE_PRESENT, List.of(ELENA), 0.0, new Random());

        assertEquals(List.of(), interjectors);
    }
}
