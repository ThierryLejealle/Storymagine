package storymagine.redacteur.coeur.domaine.orchestrator.common;

import org.junit.jupiter.api.Test;
import storymagine.redacteur.coeur.domaine.story.WorldState;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Direct unit tests for StoryFormatters.applyStateLines/entityState — written after a real bug
 * (2026-07-12) where the parser expected "ETAT: Entity: state" (a second colon) but
 * StateExtractor's prompt has always produced "ETAT: Entity → state" (an arrow). WorldState's
 * entity map was silently empty on every sequence, in every run, since the feature existed.
 */
class StoryFormattersTest {

    @Test
    void arrowFormat_populatesEntityStates() {
        WorldState ws = new WorldState();
        StoryFormatters.applyStateLines(ws, "ETAT: Thierry → en difficulté respiratoire");
        assertEquals("en difficulté respiratoire", ws.entityStates().get("Thierry"));
    }

    @Test
    void entityState_formatsPopulatedMapAsEntityColonState() {
        WorldState ws = new WorldState();
        StoryFormatters.applyStateLines(ws, "ETAT: Thierry → fatigué");
        assertEquals("Thierry : fatigué", StoryFormatters.entityState(ws));
    }

    @Test
    void multipleEtatAndEventLines_allParsed() {
        WorldState ws = new WorldState();
        StoryFormatters.applyStateLines(ws, """
                ETAT: Thierry → en difficulté respiratoire
                ETAT: Façades de Collioure → passent du rose au cuivre
                EVENT: Le soleil décline derrière les sommets.""");
        assertEquals("en difficulté respiratoire", ws.entityStates().get("Thierry"));
        assertEquals("passent du rose au cuivre", ws.entityStates().get("Façades de Collioure"));
        assertEquals(1, ws.recentEvents().size());
        assertTrue(ws.recentEvents().get(0).contains("soleil décline"));
    }

    @Test
    void aucunSentinel_leavesEntityStatesEmpty() {
        WorldState ws = new WorldState();
        StoryFormatters.applyStateLines(ws, "AUCUN");
        assertTrue(ws.entityStates().isEmpty());
        assertEquals("", StoryFormatters.entityState(ws));
    }

    @Test
    void blankOrNullInput_doesNothing() {
        WorldState ws = new WorldState();
        StoryFormatters.applyStateLines(ws, null);
        StoryFormatters.applyStateLines(ws, "   ");
        assertTrue(ws.entityStates().isEmpty());
    }

    @Test
    void emptyWorldState_entityStateReturnsBlank() {
        assertEquals("", StoryFormatters.entityState(new WorldState()));
    }
}
