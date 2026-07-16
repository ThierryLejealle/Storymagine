package storymagine.chat.coeur.domaine.scenario;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NpcTest {

    @Test
    void fullSheetCombinesPublicAndSecretInfo() {
        Npc npc = new Npc("elena", "Elena Voss", "A sharp-tongued merchant.", "Secretly a smuggler.");

        assertEquals("A sharp-tongued merchant.\n\nSecretly a smuggler.", npc.fullSheet());
    }

    @Test
    void fullSheetIsJustPublicInfoWhenThereIsNoSecret() {
        Npc npc = new Npc("elena", "Elena Voss", "A sharp-tongued merchant.", "");

        assertEquals("A sharp-tongued merchant.", npc.fullSheet());
    }

    @Test
    void labelIsTheNameWhenDeclared() {
        Npc npc = new Npc("elena", "Elena Voss", "", "");

        assertEquals("Elena Voss", npc.label());
    }

    @Test
    void labelFallsBackToIdWhenNoNameIsDeclared() {
        Npc npc = new Npc("elena", "", "", "");

        assertEquals("elena", npc.label());
    }
}
