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

class PickResolverTest {

    private static final PickLibrary PLACES = library(Map.of(
        "place", "Foggy harbor.\n---\nNight market.\n---\nEmpty town."));

    // ── Tirage ──────────────────────────────────────────────────────────────

    @Test
    void pickReplacesTheTagWithTheDrawnParagraphAndKeepsTheRestOfTheText() {
        PickResolver resolver = new PickResolver(PLACES, scripted(1));

        List<String> out = resolver.resolveAll(List.of("Alex arrives. {pick random place} The end."));

        assertEquals(List.of("Alex arrives. Night market. The end."), out);
    }

    @Test
    void drawsAreReportedInTheOrderOfTheCommandsAcrossTheTextsWithTheirMode() {
        // Les valeurs sont des indices parmi les candidats restants : 2e tirage = [0, 2][1], 3e = [0][0].
        PickResolver resolver = new PickResolver(PLACES, scripted(1, 1, 0));

        PickResolution resolution = resolver.resolve(List.of("{pick random place} / {pick unique place}", "{pick unique place}"));

        assertEquals(List.of(
            new PickDraw(PickMode.RANDOM, new PickTag("place"), List.of("Night market.")),
            new PickDraw(PickMode.UNIQUE, new PickTag("place"), List.of("Empty town.")),
            new PickDraw(PickMode.UNIQUE, new PickTag("place"), List.of("Foggy harbor."))),
            resolution.draws().values());
    }

    @Test
    void plainPickMayReturnTheSameParagraphTwice() {
        PickResolver resolver = new PickResolver(PLACES, scripted(2, 2));

        List<String> out = resolver.resolveAll(List.of("{pick random place} / {pick random place}"));

        assertEquals(List.of("Empty town. / Empty town."), out);
    }

    @Test
    void aParagraphCanSpanSeveralLinesAndItsOwnTagsAreNotResolved() {
        PickLibrary library = library(Map.of("hook", "Line one.\nLine two {pick random hook}.\n----\nOther."));
        PickResolver resolver = new PickResolver(library, scripted(0));

        assertEquals(List.of("Line one.\nLine two {pick random hook}."), resolver.resolveAll(List.of("{pick random hook}")));
    }

    // ── PICK-UNIQUE ─────────────────────────────────────────────────────────

    @Test
    void pickUniqueNeverRepeatsAParagraphEvenWhenTheRandomnessWouldPickTheSameIndex() {
        // Toujours 0 : sans l'exclusion on aurait trois fois "Foggy harbor."
        PickResolver resolver = new PickResolver(PLACES, scripted(0, 0, 0));

        List<String> out = resolver.resolveAll(List.of("{pick unique place} | {pick unique place} | {pick unique place}"));

        assertEquals(List.of("Foggy harbor. | Night market. | Empty town."), out);
    }

    @Test
    void pickUniqueExcludesWhatAPlainPickAlreadyHandedOut() {
        PickResolver resolver = new PickResolver(PLACES, scripted(0, 0));

        List<String> out = resolver.resolveAll(List.of("{pick random place} | {pick unique place}"));

        assertEquals(List.of("Foggy harbor. | Night market."), out);
    }

    @Test
    void aPlainPickIgnoresWhatPickUniqueAlreadyHandedOut() {
        PickResolver resolver = new PickResolver(PLACES, scripted(0, 0));

        List<String> out = resolver.resolveAll(List.of("{pick unique place} | {pick random place}"));

        assertEquals(List.of("Foggy harbor. | Foggy harbor."), out);
    }

    @Test
    void pickUniqueMemoryIsSharedBetweenTheTextsOfOneScenario() {
        PickResolver resolver = new PickResolver(PLACES, scripted(0, 0));

        List<String> out = resolver.resolveAll(List.of("{pick unique place}", "{pick unique place}"));

        assertEquals(List.of("Foggy harbor.", "Night market."), out);
    }

    @Test
    void pickUniqueMemoryIsPerTag() {
        PickLibrary library = library(Map.of("place", "A\n---\nB", "mood", "Gloomy\n---\nCheerful"));
        PickResolver resolver = new PickResolver(library, scripted(0, 0));

        assertEquals(List.of("A Gloomy"), resolver.resolveAll(List.of("{pick unique place} {pick unique mood}")));
    }

    @Test
    void pickUniqueFailsClearlyWhenThereAreMoreTagsThanParagraphs() {
        PickLibrary library = library(Map.of("place", "A\n---\nB"));
        PickResolver resolver = new PickResolver(library, scripted(0, 0, 0));

        IllegalStateException e = assertThrows(IllegalStateException.class,
            () -> resolver.resolveAll(List.of("{pick unique place} {pick unique place} {pick unique place}")));

        assertTrue(e.getMessage().contains("place"), e.getMessage());
    }

    // ── Modes all et N ──────────────────────────────────────────────────────

    @Test
    void allHandsOutEveryParagraphInFileOrderWithoutAnyRandomness() {
        PickResolver resolver = new PickResolver(PLACES, scripted());

        PickResolution resolution = resolver.resolve(List.of("{pick all place}"));

        assertEquals(List.of("Foggy harbor.\n\nNight market.\n\nEmpty town."), resolution.texts());
        assertEquals(List.of("Foggy harbor.", "Night market.", "Empty town."), resolution.draws().values().get(0).paragraphs());
    }

    @Test
    void aNumberHandsOutThatManyDifferentParagraphs() {
        // Indices parmi les candidats restants : [0,1,2][2] = Empty town, puis [0,1][0] = Foggy harbor.
        PickResolver resolver = new PickResolver(PLACES, scripted(2, 0));

        PickDraw draw = resolver.resolve(List.of("{pick 2 place}")).draws().values().get(0);

        assertEquals(PickMode.ofCount(2), draw.mode());
        assertEquals(List.of("Empty town.", "Foggy harbor."), draw.paragraphs());
    }

    @Test
    void aNumberLargerThanTheLibraryFailsClearly() {
        PickResolver resolver = new PickResolver(PLACES, scripted(0, 0, 0, 0));

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> resolver.resolveAll(List.of("{pick 4 place}")));

        assertTrue(e.getMessage().contains("place") && e.getMessage().contains("3"), e.getMessage());
    }

    @Test
    void whatANumberHandedOutIsExcludedFromALaterUnique() {
        // {pick 2} tire Foggy puis Night (indices 0 puis 0 parmi les restants) ; il ne reste que Empty pour unique.
        PickResolver resolver = new PickResolver(PLACES, scripted(0, 0, 0));

        List<String> out = resolver.resolveAll(List.of("{pick 2 place} | {pick unique place}"));

        assertEquals(List.of("Foggy harbor.\n\nNight market. | Empty town."), out);
    }

    @Test
    void afterAnAllNothingIsLeftForAUnique() {
        PickResolver resolver = new PickResolver(PLACES, scripted(0));

        assertThrows(IllegalStateException.class, () -> resolver.resolveAll(List.of("{pick all place} {pick unique place}")));
    }

    // ── Commandes mal ecrites ───────────────────────────────────────────────

    @Test
    void aMalformedPickCommandIsAnErrorNeverLeftInTheText() {
        PickResolver resolver = new PickResolver(PLACES, scripted(0, 0, 0));

        assertThrows(IllegalArgumentException.class, () -> resolver.resolveAll(List.of("{pick place}")));
        assertThrows(IllegalArgumentException.class, () -> resolver.resolveAll(List.of("{pick sometimes place}")));
        assertThrows(IllegalArgumentException.class, () -> resolver.resolveAll(List.of("{pick random}")));
    }

    // ── Reconnaissance des commandes ────────────────────────────────────────

    @Test
    void containsTagRecognisesPickCommandsInAnyCaseAndIgnoresOtherBraces() {
        assertTrue(PickResolver.containsTag("x {pick random place} y"));
        assertTrue(PickResolver.containsTag("{PICK Unique place}"));
        assertTrue(PickResolver.containsTag("{pick place}"));
        assertFalse(PickResolver.containsTag("{picking flowers}"));
        assertFalse(PickResolver.containsTag("[Alex enters the room.]"));
        assertFalse(PickResolver.containsTag("[NEXT ACT]"));
    }

    @Test
    void aLibraryNamedLikeAPathIsRejected() {
        PickResolver resolver = new PickResolver(PLACES, scripted(0));

        assertThrows(IllegalArgumentException.class, () -> resolver.resolveAll(List.of("{pick random ../secret}")));
        assertThrows(IllegalArgumentException.class, () -> new PickTag("../secret"));
    }

    // ── Format d'un fichier de picks ────────────────────────────────────────

    @Test
    void paragraphsAreCutOnLinesOfThreeDashesOrMoreWhateverTheLineEndings() {
        PickParagraphs p = PickParagraphs.parse("One\r\nstill one\r\n---\r\nTwo\r\n-----  \r\n\r\n---\r\nThree\r\n");

        assertEquals(List.of("One\nstill one", "Two", "Three"), p.values());
    }

    @Test
    void aDashesRunInsideALineIsNotASeparator() {
        assertEquals(1, PickParagraphs.parse("He said --- and left.").size());
    }

    // ── Outils ──────────────────────────────────────────────────────────────

    private static PickLibrary library(Map<String, String> filesByTag) {
        return tag -> PickParagraphs.parse(filesByTag.get(tag.name()));
    }

    /** Hands back the given values in order, whatever the bound. */
    private static RandomnessPort scripted(Integer... values) {
        Deque<Integer> queue = new ArrayDeque<>(List.of(values));
        return bound -> queue.pop();
    }
}
