package storymagine.chat.coeur.domaine.scenario;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CastTest {

    @Test
    void findReturnsTheNpcMatchingTheGivenId() {
        Npc elena = new Npc("elena", "Elena Voss", "A sharp-tongued merchant.", "");
        Npc marcus = new Npc("marcus", "Marcus", "A retired soldier.", "");
        Cast cast = new Cast(List.of(elena, marcus));

        assertEquals(Optional.of(elena), cast.find("elena"));
        assertEquals(Optional.of(marcus), cast.find("marcus"));
    }

    @Test
    void findIsEmptyWhenNoNpcHasThatId() {
        Cast cast = new Cast(List.of(new Npc("elena", "Elena Voss", "A sharp-tongued merchant.", "")));

        assertTrue(cast.find("marcus").isEmpty());
    }

    @Test
    void npcsPreservesConstructionOrder() {
        Npc elena = new Npc("elena", "Elena Voss", "", "");
        Npc marcus = new Npc("marcus", "Marcus", "", "");
        Cast cast = new Cast(List.of(elena, marcus));

        assertEquals(List.of(elena, marcus), cast.npcs());
        assertEquals(2, cast.size());
    }
}
