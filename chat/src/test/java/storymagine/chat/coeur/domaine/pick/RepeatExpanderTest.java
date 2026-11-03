package storymagine.chat.coeur.domaine.pick;

import org.junit.jupiter.api.Test;
import storymagine.chat.coeur.ports.RandomnessPort;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepeatExpanderTest {

    /** Three trials, each cut in two pieces : what is asked, and when it is over. */
    private static final PickLibrary TRIALS = library(Map.of(
        "trial", "Bridge.\n-\nTell a truth.\n---\nRiddle.\n-\nAnswer it.\n---\nDuel.\n-\nHit the dummy.",
        "plain", "Alpha.\n---\nBeta.\n---\nGamma.\n---\nDelta."));

    private static final String TEMPLATE = """
        Intro.
        {repeat order trial}
        # Trial {n}
        Setting: {item1}
        Quand ecrire [NEXT ACT] : Des que {item2}
        {end-repeat}
        Outro.""";

    // ── Depliage ────────────────────────────────────────────────────────────

    @Test
    void orderWithoutCountUnfoldsTheWholeFileInFileOrderWithNumberAndPieces() {
        RepeatExpansion out = new RepeatExpander(TRIALS, scripted()).expand(List.of(TEMPLATE));

        assertEquals(List.of("""
            Intro.
            # Trial 1
            Setting: Bridge.
            Quand ecrire [NEXT ACT] : Des que Tell a truth.

            # Trial 2
            Setting: Riddle.
            Quand ecrire [NEXT ACT] : Des que Answer it.

            # Trial 3
            Setting: Duel.
            Quand ecrire [NEXT ACT] : Des que Hit the dummy.
            Outro."""), out.texts());
    }

    @Test
    void orderWithACountTakesTheFirstParagraphsOnly() {
        RepeatExpansion out = new RepeatExpander(TRIALS, scripted()).expand(List.of("{repeat order 2 plain}\n{n}:{item}\n{end-repeat}"));

        assertEquals(List.of("1:Alpha.\n\n2:Beta."), out.texts());
    }

    @Test
    void randomWithACountDrawsDifferentParagraphsAndRecordsThemInPlayOrder() {
        // Indices among the candidates left : 3rd paragraph first, then the 1st of [0, 1, 3].
        RepeatExpansion out = new RepeatExpander(TRIALS, scripted(2, 0)).expand(List.of("{repeat random 2 plain}\n{n}:{item}\n{end-repeat}"));

        assertEquals(List.of("1:Gamma.\n\n2:Alpha."), out.texts());
        assertEquals(List.of(
            new PickDraw(PickMode.REPEAT_RANDOM, new PickTag("plain"), List.of("Gamma."), new PickScope(1, "plain", 1, 2)),
            new PickDraw(PickMode.REPEAT_RANDOM, new PickTag("plain"), List.of("Alpha."), new PickScope(1, "plain", 2, 2))), out.draws());
    }

    @Test
    void randomWithoutACountUsesEveryParagraphInAShuffledOrder() {
        RepeatExpansion out = new RepeatExpander(TRIALS, scripted(3, 0, 1, 0)).expand(List.of("{repeat random plain}\n{item}\n{end-repeat}"));

        assertEquals(List.of("Delta.\n\nAlpha.\n\nGamma.\n\nBeta."), out.texts());
    }

    @Test
    void itemIsTheWholeParagraphWithoutTheDashLinesAndWhatItContainsIsNotReadAsAZone() {
        PickLibrary library = library(Map.of("odd", "First {n}.\n-\nSecond {item1}.\n- not a separator"));

        RepeatExpansion out = new RepeatExpander(library, scripted()).expand(List.of("{repeat order odd}\n#{n} {item}\n{end-repeat}"));

        assertEquals(List.of("#1 First {n}.\nSecond {item1}.\n- not a separator"), out.texts());
    }

    @Test
    void aTextWithoutRepeatIsReturnedAsItIs() {
        String text = "Premise.\r\n# Act\r\n[Hello.]\r\n";

        assertEquals(List.of(text), new RepeatExpander(TRIALS, scripted()).expand(List.of(text)).texts());
        assertFalse(RepeatExpander.containsTag(text));
        assertTrue(RepeatExpander.containsTag("{repeat order trial}"));
    }

    // ── Erreurs ─────────────────────────────────────────────────────────────

    @Test
    void askingForMoreParagraphsThanTheFileHoldsFails() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> new RepeatExpander(TRIALS, scripted(0, 0, 0, 0, 0)).expand(List.of("{repeat random 5 plain}\n{item}\n{end-repeat}")));

        assertTrue(e.getMessage().contains("picks/plain.txt ne contient que 4"), e.getMessage());
    }

    @Test
    void aParagraphWithAnotherPieceCountThanTheBlockUsesFails() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> new RepeatExpander(TRIALS, scripted()).expand(List.of("{repeat order plain}\n{item1} {item2}\n{end-repeat}")));

        assertTrue(e.getMessage().contains("Alpha.") && e.getMessage().contains("1 morceau"), e.getMessage());
        // Too many is as wrong as too few : the block uses item1 only, the paragraphs hold two.
        assertThrows(IllegalArgumentException.class,
            () -> new RepeatExpander(TRIALS, scripted()).expand(List.of("{repeat order trial}\n{item1}\n{end-repeat}")));
    }

    @Test
    void zonesOutsideABlockAndMalformedBlocksFailWithTheLineNumber() {
        RepeatExpander expander = new RepeatExpander(TRIALS, scripted());

        assertTrue(assertThrows(IllegalArgumentException.class, () -> expander.expand(List.of("A\nTrial {n}"))).getMessage().startsWith("ligne 2"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> expander.expand(List.of("{repeat order plain}\n{item}"))).getMessage().contains("sans {end-repeat}"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> expander.expand(List.of("{end-repeat}"))).getMessage().contains("sans {repeat}"));
        assertTrue(assertThrows(IllegalArgumentException.class,
            () -> expander.expand(List.of("{repeat order plain}\n{repeat order plain}\n{end-repeat}\n{end-repeat}"))).getMessage().contains("contenir un autre"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> expander.expand(List.of("Text {repeat order plain}\n{item}\n{end-repeat}"))).getMessage().contains("seuls sur leur ligne"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> expander.expand(List.of("{repeat plain}\n{item}\n{end-repeat}"))).getMessage().contains("Commande repeat invalide"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> expander.expand(List.of("{repeat sometimes plain}\n{item}\n{end-repeat}"))).getMessage().contains("order ou random"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> expander.expand(List.of("{repeat order plain}\n{end-repeat}"))).getMessage().contains("bloc vide"));
    }

    // ── Avec pick ───────────────────────────────────────────────────────────

    @Test
    void aPickInsideABlockIsDrawnOncePerCopyAndEachCopyGroupsItsItemAndItsPick() {
        PickLibrary library = library(Map.of("plain", "Alpha.\n---\nBeta.", "place", "Harbor.\n---\nMarket."));
        // repeat order draws nothing ; the two copies then draw "place" : Market (index 1), Harbor (index 0).
        PickResolver resolver = new PickResolver(library, scripted(1, 0));

        PickResolution resolution = resolver.resolve(List.of("{repeat order plain}\n{item} at {pick random place}\n{end-repeat}"));

        assertEquals(List.of("Alpha. at Market.\n\nBeta. at Harbor."), resolution.texts());
        assertEquals(List.of(
            new PickDraw(PickMode.REPEAT_ORDER, new PickTag("plain"), List.of("Alpha."), new PickScope(1, "plain", 1, 2)),
            new PickDraw(PickMode.RANDOM, new PickTag("place"), List.of("Market."), new PickScope(1, "plain", 1, 2)),
            new PickDraw(PickMode.REPEAT_ORDER, new PickTag("plain"), List.of("Beta."), new PickScope(1, "plain", 2, 2)),
            new PickDraw(PickMode.RANDOM, new PickTag("place"), List.of("Harbor."), new PickScope(1, "plain", 2, 2))),
            resolution.draws().values());
        assertTrue(PickResolver.containsTag("{repeat order plain}"));
    }

    private static PickLibrary library(Map<String, String> filesByTag) {
        return tag -> PickParagraphs.parse(filesByTag.get(tag.name()));
    }

    /** Hands back the given values in order, whatever the bound. */
    private static RandomnessPort scripted(Integer... values) {
        Deque<Integer> queue = new ArrayDeque<>(List.of(values));
        return bound -> queue.pop();
    }
}
