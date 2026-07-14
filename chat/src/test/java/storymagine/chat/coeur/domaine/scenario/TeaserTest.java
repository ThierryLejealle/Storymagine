package storymagine.chat.coeur.domaine.scenario;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeaserTest {

    @Test
    void extractsASingleBracketedLine() {
        String text = "Act 2 — The searched house\n[Sylka holds her breath at the creaking door.]\n\nStarting point: ...";
        assertEquals(List.of("Sylka holds her breath at the creaking door."), Teaser.extractAll(text));
    }

    @Test
    void extractsSeveralBracketedLinesInSourceOrder() {
        String text = """
            Act 3 — The map
            [The two thieves found the temple's map in the priest's house.]

            What should happen: they study the map...

            [They decide to take a day to prepare the next step.]

            When to write [NEXT ACT]: ...""";

        assertEquals(List.of(
            "The two thieves found the temple's map in the priest's house.",
            "They decide to take a day to prepare the next step."
        ), Teaser.extractAll(text));
    }

    @Test
    void returnsEmptyListWhenNoBracketedLineIsPresent() {
        assertTrue(Teaser.extractAll("Act 1 — The priest\n\nStarting point: ...").isEmpty());
    }

    @Test
    void returnsEmptyListForNullText() {
        assertTrue(Teaser.extractAll(null).isEmpty());
    }

    @Test
    void ignoresBracketsThatAreNotAloneOnTheirLine() {
        String text = "Act 1\nHe glances aside [nervously] at the door.\n";
        assertTrue(Teaser.extractAll(text).isEmpty());
    }

    @Test
    void extractsABracketedBlockSpanningSeveralLines() {
        String text = """
            Act 1 — The priest
            [You and Sylka, your longtime partner in jobs like this, have been hired by a
            shadowy patron to bring back the Tear of Vaskorreth intact.]

            Starting point: ...""";

        assertEquals(List.of(
            "You and Sylka, your longtime partner in jobs like this, have been hired by a "
                + "shadowy patron to bring back the Tear of Vaskorreth intact."
        ), Teaser.extractAll(text));
    }

    @Test
    void extractsAMultiLineBlockFollowedByASingleLineBlock() {
        String text = """
            Act 1
            [First line of the block
            continues here.]
            [A second, single-line block.]

            Starting point: ...""";

        assertEquals(List.of(
            "First line of the block continues here.",
            "A second, single-line block."
        ), Teaser.extractAll(text));
    }

    @Test
    void stillIgnoresAnInlineNextActMarkerNextToAMultiLineBlock() {
        String text = """
            Act 1
            [A multi-line
            beat here.]

            When to write [NEXT ACT]: once the condition is met.""";

        assertEquals(List.of("A multi-line beat here."), Teaser.extractAll(text));
    }
}
