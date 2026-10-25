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

        List<String> out = resolver.resolveAll(List.of("Alex arrives. [PICK: place] The end."));

        assertEquals(List.of("Alex arrives. Night market. The end."), out);
    }

    @Test
    void plainPickMayReturnTheSameParagraphTwice() {
        PickResolver resolver = new PickResolver(PLACES, scripted(2, 2));

        List<String> out = resolver.resolveAll(List.of("[PICK: place] / [PICK: place]"));

        assertEquals(List.of("Empty town. / Empty town."), out);
    }

    @Test
    void aParagraphCanSpanSeveralLinesAndItsOwnTagsAreNotResolved() {
        PickLibrary library = library(Map.of("hook", "Line one.\nLine two [PICK: hook].\n----\nOther."));
        PickResolver resolver = new PickResolver(library, scripted(0));

        assertEquals(List.of("Line one.\nLine two [PICK: hook]."), resolver.resolveAll(List.of("[PICK: hook]")));
    }

    // ── PICK-UNIQUE ─────────────────────────────────────────────────────────

    @Test
    void pickUniqueNeverRepeatsAParagraphEvenWhenTheRandomnessWouldPickTheSameIndex() {
        // Toujours 0 : sans l'exclusion on aurait trois fois "Foggy harbor."
        PickResolver resolver = new PickResolver(PLACES, scripted(0, 0, 0));

        List<String> out = resolver.resolveAll(List.of("[PICK-UNIQUE: place] | [PICK-UNIQUE: place] | [PICK-UNIQUE: place]"));

        assertEquals(List.of("Foggy harbor. | Night market. | Empty town."), out);
    }

    @Test
    void pickUniqueExcludesWhatAPlainPickAlreadyHandedOut() {
        PickResolver resolver = new PickResolver(PLACES, scripted(0, 0));

        List<String> out = resolver.resolveAll(List.of("[PICK: place] | [PICK-UNIQUE: place]"));

        assertEquals(List.of("Foggy harbor. | Night market."), out);
    }

    @Test
    void aPlainPickIgnoresWhatPickUniqueAlreadyHandedOut() {
        PickResolver resolver = new PickResolver(PLACES, scripted(0, 0));

        List<String> out = resolver.resolveAll(List.of("[PICK-UNIQUE: place] | [PICK: place]"));

        assertEquals(List.of("Foggy harbor. | Foggy harbor."), out);
    }

    @Test
    void pickUniqueMemoryIsSharedBetweenTheTextsOfOneScenario() {
        PickResolver resolver = new PickResolver(PLACES, scripted(0, 0));

        List<String> out = resolver.resolveAll(List.of("[PICK-UNIQUE: place]", "[PICK-UNIQUE: place]"));

        assertEquals(List.of("Foggy harbor.", "Night market."), out);
    }

    @Test
    void pickUniqueMemoryIsPerTag() {
        PickLibrary library = library(Map.of("place", "A\n---\nB", "mood", "Gloomy\n---\nCheerful"));
        PickResolver resolver = new PickResolver(library, scripted(0, 0));

        assertEquals(List.of("A Gloomy"), resolver.resolveAll(List.of("[PICK-UNIQUE: place] [PICK-UNIQUE: mood]")));
    }

    @Test
    void pickUniqueFailsClearlyWhenThereAreMoreTagsThanParagraphs() {
        PickLibrary library = library(Map.of("place", "A\n---\nB"));
        PickResolver resolver = new PickResolver(library, scripted(0, 0, 0));

        IllegalStateException e = assertThrows(IllegalStateException.class,
            () -> resolver.resolveAll(List.of("[PICK-UNIQUE: place] [PICK-UNIQUE: place] [PICK-UNIQUE: place]")));

        assertTrue(e.getMessage().contains("place"), e.getMessage());
    }

    // ── Reconnaissance des tags ─────────────────────────────────────────────

    @Test
    void containsTagRecognisesBothFormsAndIgnoresOrdinaryBrackets() {
        assertTrue(PickResolver.containsTag("x [PICK: place] y"));
        assertTrue(PickResolver.containsTag("[pick-unique:place]"));
        assertFalse(PickResolver.containsTag("[Alex enters the room.]"));
        assertFalse(PickResolver.containsTag("[NEXT ACT]"));
    }

    @Test
    void aTagNamingAPathIsNeverTreatedAsATag() {
        assertFalse(PickResolver.containsTag("[PICK: ../secret]"));
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
