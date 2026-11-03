package storymagine.chat.coeur.domaine.pick;

import org.junit.jupiter.api.Test;
import storymagine.chat.coeur.domaine.scenario.Cast;
import storymagine.chat.coeur.domaine.scenario.Npc;
import storymagine.chat.coeur.ports.RandomnessPort;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** "{cast}" and "%NAME%" through the whole text resolution (repeat unfolding, picks, then casts). No LLM. */
class CastResolutionTest {

    private static final Cast THREE = new Cast(List.of(
        new Npc("jack", "Jack", "", ""), new Npc("mia", "Mia", "", ""), new Npc("john", "John", "", "")));
    private static final Cast ONE = new Cast(List.of(new Npc("jack", "Jack", "", "")));

    private static final PickLibrary LIBRARY = tag -> PickParagraphs.parse(
        Map.of("evenement", "Un orage eclate. %A% se met a l'abri.").get(tag.name()));

    private static RandomnessPort seeded(long seed) {
        return new Random(seed)::nextInt;
    }

    private static String resolve(String text, Cast cast, long seed) {
        return new PickResolver(LIBRARY, seeded(seed), cast).resolveAll(List.of(text)).get(0);
    }

    private static List<String> linesOf(String text) {
        return text.lines().filter(l -> !l.isBlank()).toList();
    }

    // ── Exclusivite ─────────────────────────────────────────────────────────

    @Test
    void twoVariablesNeverHoldTheSameCharacterByDefault() {
        for (long seed = 0; seed < 50; seed++) {
            String out = resolve("{cast A}\n{cast B}\n%A%/%B%", THREE, seed);
            String[] pair = linesOf(out).get(0).split("/");
            assertTrue(!pair[0].equals(pair[1]), "graine " + seed + " : " + out);
        }
    }

    @Test
    void reusePossibleLetsAVariableTakeAnotherOnesCharacter() {
        assertEquals("Jack/Jack", linesOf(resolve("{cast A}\n{cast B reuse-possible}\n%A%/%B%", ONE, 1)).get(0));

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> resolve("{cast A}\n{cast B}\n%A%/%B%", ONE, 1));
        assertTrue(e.getMessage().contains("{cast B}"), e.getMessage());
    }

    @Test
    void theListRestrictsTheDraw() {
        for (long seed = 0; seed < 30; seed++) {
            String out = resolve("{cast A (jack, mia)}\n%A%", THREE, seed);
            assertTrue(out.strip().equals("Jack") || out.strip().equals("Mia"), "graine " + seed + " : " + out);
        }
    }

    @Test
    void aDrawThatLeavesALaterCommandWithNobodyIsUndoneAndAnotherTried() {
        // B can only be Jack, so A must be Mia : whatever A tries first, the search ends on that one answer.
        for (long seed = 0; seed < 50; seed++) {
            String out = resolve("{cast A (jack, mia)}\n{cast B (jack)}\n%A%/%B%", THREE, seed);
            assertEquals("Mia/Jack", out.strip(), "graine " + seed);
        }
    }

    @Test
    void anImpossibleCombinationIsReportedWithTheCommandThatFails() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
            () -> resolve("{cast A (jack)}\n{cast B (jack)}\n%A%", THREE, 0));

        assertTrue(e.getMessage().contains("{cast B (jack)}"), e.getMessage());
        assertTrue(e.getMessage().contains("aucune combinaison"), e.getMessage());
    }

    // ── Dans un repeat ──────────────────────────────────────────────────────

    @Test
    void uniqueGivesADifferentCharacterAtEachCopyOfARepeat() {
        for (long seed = 0; seed < 30; seed++) {
            String out = resolve("{repeat 3}\n{cast X unique}\n%X%\n{end-repeat}", THREE, seed);
            assertEquals(Set.of("Jack", "Mia", "John"), Set.copyOf(linesOf(out)), "graine " + seed + " : " + out);
        }
        assertThrows(IllegalStateException.class, () -> resolve("{repeat 4}\n{cast X unique}\n%X%\n{end-repeat}", THREE, 0));
    }

    @Test
    void withoutUniqueACopyMayTakeTheSameCharacterAgain() {
        boolean someRepeat = IntStream.range(0, 40).anyMatch(seed -> {
            List<String> names = linesOf(resolve("{repeat 3}\n{cast X}\n%X%\n{end-repeat}", THREE, seed));
            return Set.copyOf(names).size() < 3;
        });

        assertTrue(someRepeat);
    }

    @Test
    void repeatOverTheCastBindsTheVariableAndTheOtherOneIsNeverTheSameCharacter() {
        String template = "{repeat order cast HERO}\n## Epreuve {n}\n{cast PARTNER}\n%HERO%+%PARTNER%\n{end-repeat}";
        for (long seed = 0; seed < 50; seed++) {
            List<String> pairs = linesOf(resolve(template, THREE, seed)).stream().filter(l -> l.contains("+")).toList();

            assertEquals(3, pairs.size(), "graine " + seed);
            assertEquals(List.of("Jack", "Mia", "John"), pairs.stream().map(p -> p.split("\\+")[0]).toList(), "dans l'ordre du casting");
            for (String pair : pairs) {
                String[] names = pair.split("\\+");
                assertTrue(!names[0].equals(names[1]), "graine " + seed + " : " + pair);
            }
        }
    }

    @Test
    void repeatOverAListedPartOfTheCastWithACount() {
        String out = resolve("{repeat random 2 cast HERO (jack, mia, john)}\n%HERO%\n{end-repeat}", THREE, 7);

        List<String> names = linesOf(out);
        assertEquals(2, names.size());
        assertEquals(2, Set.copyOf(names).size(), "deux personnages differents");
    }

    @Test
    void aVariableDeclaredInsideACopyIsForgottenAfterIt() {
        String text = "{repeat 2}\n{cast PARTNER}\n%PARTNER%\n{end-repeat}\n%PARTNER%";

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> resolve(text, THREE, 0));
        assertTrue(e.getMessage().contains("%PARTNER%"), e.getMessage());
    }

    @Test
    void aVariableDeclaredBeforeARepeatIsStillThereInsideAndItsCharacterIsExcluded() {
        for (long seed = 0; seed < 40; seed++) {
            String out = resolve("{cast BOSS (mia)}\n{repeat 2}\n{cast X}\n%BOSS%:%X%\n{end-repeat}", THREE, seed);
            for (String line : linesOf(out)) {
                assertTrue(line.startsWith("Mia:") && !line.equals("Mia:Mia"), "graine " + seed + " : " + line);
            }
        }
    }

    @Test
    void repeatWithoutALibraryHasNoItem() {
        assertEquals(List.of("Etape 1", "Etape 2"), linesOf(resolve("{repeat 2}\nEtape {n}\n{end-repeat}", THREE, 0)));
        assertThrows(IllegalArgumentException.class, () -> resolve("{repeat 2}\nEtape {item}\n{end-repeat}", THREE, 0));
        assertThrows(IllegalArgumentException.class, () -> resolve("{repeat 0}\nx\n{end-repeat}", THREE, 0));
    }

    // ── Le texte produit ────────────────────────────────────────────────────

    @Test
    void commandLinesAndInternalLinesAreRemovedAndTheNameIsPutInsideCommands() {
        String out = resolve("{cast A (jack)}\n{npc-starts %A%}\nTexte.", THREE, 0);

        assertEquals(List.of("{npc-starts Jack}", "Texte."), linesOf(out));
        assertFalse(out.contains("copy-") || out.contains("cast-bind") || out.contains("{cast"), out);
    }

    @Test
    void aVariableUsedBeforeItsDeclarationFails() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> resolve("%A% entre.\n{cast A}", THREE, 0));

        assertTrue(e.getMessage().contains("%A%"), e.getMessage());
    }

    @Test
    void aVariableWrittenInADrawnParagraphIsReplacedToo() {
        String out = resolve("{cast A (mia)}\n{pick random evenement}", THREE, 0);

        assertEquals("Un orage eclate. Mia se met a l'abri.", out.strip());
    }

    @Test
    void anUnknownOrAmbiguousCharacterInTheListFailsWithTheCommand() {
        Cast twins = new Cast(List.of(new Npc("marc1", "Marc Dupont", "", ""), new Npc("marc2", "Marc Leroy", "", "")));

        IllegalArgumentException unknown = assertThrows(IllegalArgumentException.class, () -> resolve("{cast A (zorglub)}", THREE, 0));
        assertTrue(unknown.getMessage().contains("zorglub") && unknown.getMessage().contains("{cast A (zorglub)}"), unknown.getMessage());
        IllegalArgumentException ambiguous = assertThrows(IllegalArgumentException.class, () -> resolve("{cast A (marc)}", twins, 0));
        assertTrue(ambiguous.getMessage().contains("marc1") && ambiguous.getMessage().contains("marc2"), ambiguous.getMessage());
        assertThrows(IllegalArgumentException.class, () -> resolve("{cast A (jack, Jack)}", THREE, 0));
    }

    @Test
    void everyCastIsRecordedForTheDrawsTable() {
        PickResolution resolution = new PickResolver(LIBRARY, seeded(3), THREE).resolve(List.of("{cast A (mia)}\n{cast B (jack)}\n%A%"));

        List<PickDraw> casts = resolution.draws().values().stream().filter(d -> d.mode().equals(PickMode.CAST)).toList();
        assertEquals(List.of("A", "B"), casts.stream().map(d -> d.tag().name()).collect(Collectors.toList()));
        assertEquals(List.of(List.of("Mia"), List.of("Jack")), casts.stream().map(PickDraw::paragraphs).toList());
    }
}
