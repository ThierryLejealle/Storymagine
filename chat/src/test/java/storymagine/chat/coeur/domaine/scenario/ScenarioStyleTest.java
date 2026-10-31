package storymagine.chat.coeur.domaine.scenario;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The {N%} chance markers of style.txt : a line is kept without its marker, or dropped whole. */
class ScenarioStyleTest {

    private static final String STYLE = """
        Short sentences.
        - {20%} Praise the outfit of one of the girls.
        - Each girl keeps her own voice. {50%}
        """;

    @Test
    void aStyleWithoutMarkerIsReturnedAsItIs() {
        ScenarioStyle plain = ScenarioStyle.of("Short sentences.\n- Plain words.");

        assertSame(plain, plain.drawn(() -> 0.0));
    }

    @Test
    void aLineWhoseDrawIsLuckyKeepsItsPlaceWithoutTheMarker() {
        ScenarioStyle drawn = ScenarioStyle.of(STYLE).drawn(() -> 0.0);

        assertEquals("""
            Short sentences.
            - Praise the outfit of one of the girls.
            - Each girl keeps her own voice.""", drawn.text());
    }

    @Test
    void aLineWhoseDrawFailsDisappearsCompletelyDashIncluded() {
        ScenarioStyle drawn = ScenarioStyle.of(STYLE).drawn(() -> 0.999);

        assertEquals("Short sentences.", drawn.text());
    }

    @Test
    void eachMarkedLineIsDrawnOnItsOwnWithItsOwnPercentage() {
        // 0.30 : trop grand pour {20%}, assez petit pour {50%}
        ScenarioStyle drawn = ScenarioStyle.of(STYLE).drawn(() -> 0.30);

        assertEquals("Short sentences.\n- Each girl keeps her own voice.", drawn.text());
    }

    @Test
    void theMarkerCanSitAnywhereInTheLine() {
        assertEquals("Add a smell, rarely.", ScenarioStyle.of("{100%} Add a smell, rarely.").drawn(() -> 0.5).text());
        assertEquals("Add a smell, rarely.", ScenarioStyle.of("Add a smell, {100%} rarely.").drawn(() -> 0.5).text());
        assertEquals("Add a smell, rarely.", ScenarioStyle.of("Add a smell, rarely. {100%}").drawn(() -> 0.5).text());
    }

    @Test
    void whenEveryLineIsDroppedTheStyleIsAbsent() {
        ScenarioStyle drawn = ScenarioStyle.of("{10%} Only a rare hint.").drawn(() -> 0.9);

        assertTrue(!drawn.isPresent());
    }

    @Test
    void fourLinesAtTenPercentEachAreDrawnIndependentlyWithTheExpectedFrequencies() {
        ScenarioStyle style = ScenarioStyle.of("""
            Base.
            - {10%} Line A.
            - {10%} Line B.
            - {10%} Line C.
            - {10%} Line D.
            """);
        Random random = new Random(2024);
        int draws = 40_000;
        int[] perLine = new int[4];
        int[] byCount = new int[5]; // combien de lignes rares sont sorties ensemble
        for (int i = 0; i < draws; i++) {
            String drawn = style.drawn(random::nextDouble).text();
            int lines = 0;
            for (int l = 0; l < 4; l++) {
                if (drawn.contains("Line " + (char) ('A' + l))) { perLine[l]++; lines++; }
            }
            byCount[lines]++;
            assertTrue(drawn.startsWith("Base."), "la ligne sans marqueur est toujours la");
        }

        for (int l = 0; l < 4; l++) {
            assertEquals(0.10, perLine[l] / (double) draws, 0.01, "frequence de la ligne " + (char) ('A' + l));
        }
        // lignes independantes : repartition binomiale(4, 0.10) = 65.6 %, 29.2 %, 4.9 %, 0.4 %, 0.01 %
        assertEquals(0.6561, byCount[0] / (double) draws, 0.015, "aucune ligne rare");
        assertEquals(0.2916, byCount[1] / (double) draws, 0.015, "une seule ligne rare");
        assertEquals(0.0486, byCount[2] / (double) draws, 0.01, "deux lignes rares");
    }

    @Test
    void theLongRunFrequencyMatchesThePercentage() {
        ScenarioStyle style = ScenarioStyle.of("Base.\n{25%} Rare hint.");
        Random random = new Random(5);
        int kept = 0;
        for (int i = 0; i < 4000; i++) {
            if (style.drawn(random::nextDouble).text().contains("Rare hint.")) kept++;
        }

        assertEquals(0.25, kept / 4000.0, 0.03);
    }
}
