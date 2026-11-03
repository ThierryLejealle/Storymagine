package storymagine.chat.coeur.domaine.scenario;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StoryDirectiveMarkerTest {

    @Test
    void commandsAreReadInOrderAndTheirLinesAreCutOutOfTheText() {
        StoryDirectiveMarker.Extracted e = StoryDirectiveMarker.extract(
            "Point de départ : la nuit.\n{npc-story on Léa froide}\nSuite.\n  {NPC-STORY Off  Léa   froide }\n");

        assertEquals(List.of(new StoryDirective(true, "Léa", "froide"), new StoryDirective(false, "Léa", "froide")), e.directives());
        assertEquals("Point de départ : la nuit.\nSuite.\n", e.text());
    }

    @Test
    void theTagIsTheLastWordSoACharacterNameMayHaveSeveralWords() {
        StoryDirectiveMarker.Extracted e = StoryDirectiveMarker.extract("{npc-story on Marcus Voss trahison}\n");

        assertEquals(List.of(new StoryDirective(true, "Marcus Voss", "trahison")), e.directives());
    }

    @Test
    void aTextWithoutCommandIsUntouched() {
        StoryDirectiveMarker.Extracted e = StoryDirectiveMarker.extract("[Alex entre.]\nCorps.");

        assertTrue(e.directives().isEmpty());
        assertEquals("[Alex entre.]\nCorps.", e.text());
    }

    @Test
    void aCommandNotAloneOnItsLineOrIncompleteIsAnErrorNeverKeptInTheText() {
        assertThrows(IllegalArgumentException.class, () -> StoryDirectiveMarker.extract("Alex dit {npc-story on Léa froide} puis part.\n"));
        assertThrows(IllegalArgumentException.class, () -> StoryDirectiveMarker.extract("{npc-story on Léa}\n"));
        assertThrows(IllegalArgumentException.class, () -> StoryDirectiveMarker.extract("{npc-story toggle Léa froide}\n"));
    }
}
