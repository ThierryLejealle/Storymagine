package storymagine.chat.coeur.domaine.pick;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CastCommandTest {

    @Test
    void aBareCommandHasNoKeywordAndNoList() {
        CastCommand command = CastCommand.parseLine("  {cast PNJ_1}  ").orElseThrow();

        assertEquals("PNJ_1", command.variable());
        assertFalse(command.unique());
        assertFalse(command.reusePossible());
        assertTrue(command.candidates().isEmpty());
    }

    @Test
    void keywordsAndListCanBeCombinedInAnyCaseAndOrder() {
        CastCommand command = CastCommand.parseLine("{CAST Boss reuse-possible UNIQUE (jack, mia ,john)}").orElseThrow();

        assertTrue(command.unique());
        assertTrue(command.reusePossible());
        assertEquals(List.of("jack", "mia", "john"), command.candidates());
    }

    @Test
    void aLineWithoutCastIsNotACommand() {
        assertTrue(CastCommand.parseLine("Jack entre.").isEmpty());
        assertTrue(CastCommand.parseLine("{cast-bind A jack}").isEmpty(), "une ligne interne n'est pas une commande de l'auteur");
        assertTrue(CastCommand.parseLine("{pick random lieu}").isEmpty());
    }

    @Test
    void aBadlyWrittenCommandFailsWithWhatIsExpected() {
        IllegalArgumentException keyword = assertThrows(IllegalArgumentException.class, () -> CastCommand.parseLine("{cast A free}"));
        assertTrue(keyword.getMessage().contains("free") && keyword.getMessage().contains("reuse-possible"), keyword.getMessage());

        assertThrows(IllegalArgumentException.class, () -> CastCommand.parseLine("{cast}"));
        assertThrows(IllegalArgumentException.class, () -> CastCommand.parseLine("{cast 1A}"));
        assertThrows(IllegalArgumentException.class, () -> CastCommand.parseLine("Alors {cast A} arrive."));
        assertThrows(IllegalArgumentException.class, () -> CastCommand.parseLine("{cast A ()}"));
        assertThrows(IllegalArgumentException.class, () -> CastCommand.parseLine("{cast A (jack,)}"));
        assertThrows(IllegalArgumentException.class, () -> CastCommand.parseLine("{cast A (jack, mia brown)}"));
    }
}
