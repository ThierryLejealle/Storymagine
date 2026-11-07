package storymagine.chat.coeur.domaine.scenario;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CharacterDeltaMarkerTest {

    @Test
    void deltasAreReadInOrderAndTheirLinesAreCutOutOfTheText() {
        CharacterDeltaMarker.Extracted e = CharacterDeltaMarker.extract(
            "Point de départ : la nuit.\n{npc-delta public Elena : Elle a perdu un œil.}\nSuite.\n"
                + "  {NPC-DELTA Secret  Marcus Voss : Il décide de trahir le groupe. }\n");

        assertEquals(List.of(
            new CharacterDelta(CharacterDelta.Visibility.PUBLIC, "Elena", "Elle a perdu un œil."),
            new CharacterDelta(CharacterDelta.Visibility.SECRET, "Marcus Voss", "Il décide de trahir le groupe.")),
            e.deltas());
        assertEquals("Point de départ : la nuit.\nSuite.\n", e.text());
    }

    @Test
    void theBracesDelimitTheCommandSoItsTextMaySpanSeveralLines() {
        CharacterDeltaMarker.Extracted e = CharacterDeltaMarker.extract("""
            Avant.
            {npc-delta secret Marcus : Il a reconnu le chef.
                Il décide de le suivre,
            sans rien dire aux autres.
            }
            Après.
            """);

        assertEquals(List.of(new CharacterDelta(CharacterDelta.Visibility.SECRET, "Marcus",
            "Il a reconnu le chef.\nIl décide de le suivre,\nsans rien dire aux autres.")), e.deltas());
        assertEquals("Avant.\nAprès.\n", e.text());
    }

    @Test
    void aMultiLineDeltaNeverClosedIsAnError() {
        assertThrows(IllegalArgumentException.class, () -> CharacterDeltaMarker.extract(
            "{npc-delta secret Marcus : Il a reconnu le chef.\nAction suivante sans accolade fermante.\n"));
    }

    @Test
    void aTextWithoutDeltaIsUntouched() {
        CharacterDeltaMarker.Extracted e = CharacterDeltaMarker.extract("[Alex entre.]\nCorps.");

        assertTrue(e.deltas().isEmpty());
        assertEquals("[Alex entre.]\nCorps.", e.text());
    }

    @Test
    void aDeltaThatCouldLeakIsAnErrorNeverKeptInTheText() {
        // Visibilite manquante, texte manquant, au milieu d'une phrase : jamais laisse tel quel dans le texte de l'acte.
        for (String bad : new String[] {
                "{npc-delta Elena : sans visibilite}",
                "{npc-delta secret Elena}",
                "Il se passe {npc-delta secret Elena : fuite} au milieu d'une phrase."}) {
            assertThrows(IllegalArgumentException.class, () -> CharacterDeltaMarker.extract(bad), bad);
        }
    }
}
