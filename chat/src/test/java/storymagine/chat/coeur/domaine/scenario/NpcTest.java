package storymagine.chat.coeur.domaine.scenario;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

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

    private static Npc withTags() {
        return new Npc("lea", "Léa", "Réservée.", "Elle déteste se tromper.", List.of(
            new StoryTag("froide", CharacterDelta.Visibility.PUBLIC, "Distante."),
            new StoryTag("amicale", CharacterDelta.Visibility.PUBLIC, "Souriante."),
            new StoryTag("chatons", CharacterDelta.Visibility.SECRET, "Touchée.")));
    }

    @Test
    void withoutActiveTagsTheSheetIsOnlyItsPermanentText() {
        Npc npc = withTags().withActiveTags(Set.of());

        assertEquals("Réservée.", npc.publicInfo());
        assertEquals("Elle déteste se tromper.", npc.secretInfo());
    }

    @Test
    void anActiveTagGoesToThePublicOrSecretSheetAccordingToItsVisibilityInFileOrder() {
        Npc npc = withTags().withActiveTags(Set.of("chatons", "amicale", "froide"));

        assertEquals("Réservée.\n\nDistante.\n\nSouriante.", npc.publicInfo());
        assertEquals("Elle déteste se tromper.\n\nTouchée.", npc.secretInfo());
    }

    @Test
    void aSecretTagNeverReachesThePublicSheetAndATagOfAnEmptySecretStartsIt() {
        Npc npc = new Npc("lea", "Léa", "Réservée.", "", List.of(new StoryTag("dette", CharacterDelta.Visibility.SECRET, "Elle doit de l'argent.")))
            .withActiveTags(Set.of("dette"));

        assertEquals("Réservée.", npc.publicInfo());
        assertEquals("Elle doit de l'argent.", npc.secretInfo());
    }

    @Test
    void deltasKeepTheTagsOfTheSheet() {
        Npc npc = withTags().withPublicAddition("Elle boite.").withSecretAddition("Elle ment.");

        assertEquals(3, npc.tags().size());
    }
}
