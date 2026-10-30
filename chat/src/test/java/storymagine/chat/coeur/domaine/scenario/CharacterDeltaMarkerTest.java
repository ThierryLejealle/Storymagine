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
            "Point de départ : la nuit.\n{delta-pnj public Elena : Elle a perdu un œil.}\nSuite.\n"
                + "  {DELTA-PNJ Secret  Marcus Voss : Il décide de trahir le groupe. }\n");

        assertEquals(List.of(
            new CharacterDelta(CharacterDelta.Visibility.PUBLIC, "Elena", "Elle a perdu un œil."),
            new CharacterDelta(CharacterDelta.Visibility.SECRET, "Marcus Voss", "Il décide de trahir le groupe.")),
            e.deltas());
        assertEquals("Point de départ : la nuit.\nSuite.\n", e.text());
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
                "{delta-pnj Elena : sans visibilite}",
                "{delta-pnj secret Elena}",
                "Il se passe {delta-pnj secret Elena : fuite} au milieu d'une phrase."}) {
            assertThrows(IllegalArgumentException.class, () -> CharacterDeltaMarker.extract(bad), bad);
        }
    }
}
