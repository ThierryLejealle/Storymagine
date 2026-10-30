package storymagine.chat.coeur.domaine.scenario;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcOpeningMarkerTest {

    @Test
    void bareMarkerMeansAnyNpcAndTheLineIsCutOut() {
        NpcOpeningMarker.Extracted e = NpcOpeningMarker.extract("Monde : un chateau.\n{npc-starts}\nTon : sombre.");

        assertEquals(NpcOpening.ANY, e.opening());
        assertEquals("Monde : un chateau.\nTon : sombre.", e.text());
    }

    @Test
    void markerWithNameNamesTheNpc() {
        NpcOpening opening = NpcOpeningMarker.extract("{npc-starts Elena Voss }").opening();

        assertTrue(opening.isNamed());
        assertEquals("Elena Voss", opening.npcName());
    }

    @Test
    void caseAndSurroundingSpacesAreTolerated() {
        assertTrue(NpcOpeningMarker.extract("{NPC-Starts}").opening().npcStarts());
        assertTrue(NpcOpeningMarker.extract("  {npc-starts   Marcus}  ").opening().isNamed());
    }

    @Test
    void theOldBracketSpellingIsNoLongerACommand() {
        assertTrue(NpcOpeningMarker.extract("[NPC STARTS]").opening().isNone());
    }

    @Test
    void aMentionInsideASentenceIsNotAMarker() {
        String note = "(Pour qu'un PNJ ouvre, écrire {npc-starts} seul sur une ligne.)";

        NpcOpeningMarker.Extracted e = NpcOpeningMarker.extract(note);

        assertTrue(e.opening().isNone());
        assertEquals(note, e.text());
    }

    @Test
    void theFirstMarkerDecidesAndEveryMarkerLineIsRemoved() {
        NpcOpeningMarker.Extracted e = NpcOpeningMarker.extract("{npc-starts Marcus}\ntexte\n{npc-starts Elena}\n");

        assertEquals("Marcus", e.opening().npcName());
        assertEquals("texte\n", e.text());
    }

    @Test
    void noMarkerLeavesTheTextUntouched() {
        NpcOpeningMarker.Extracted e = NpcOpeningMarker.extract("[Alex entre dans la salle.]");

        assertTrue(e.opening().isNone());
        assertEquals("[Alex entre dans la salle.]", e.text());
    }
}
