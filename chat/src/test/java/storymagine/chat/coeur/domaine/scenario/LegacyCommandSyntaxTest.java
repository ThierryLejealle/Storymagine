package storymagine.chat.coeur.domaine.scenario;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyCommandSyntaxTest {

    @Test
    void everyOldBracketCommandIsRefusedWithAHintToTheNewSyntax() {
        for (String old : new String[] {"[PICK: place]", "[pick-unique:place]", "[NPC STARTS]", "[NPC STARTS: Elena]", "[npc_starts]"}) {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> LegacyCommandSyntax.requireNone("Texte.\n" + old + "\nSuite."), old);
            assertTrue(e.getMessage().contains("accolades"), e.getMessage());
        }
    }

    @Test
    void nextActStoryBeatsAndNewCommandsAreNotLegacy() {
        assertDoesNotThrow(() -> LegacyCommandSyntax.requireNone(
            "[Alex entre.]\nQuand écrire [NEXT ACT] : à la porte.\n{pick random lieu}\n{npc-starts Elena}"));
        assertDoesNotThrow(() -> LegacyCommandSyntax.requireNone(null));
    }
}
