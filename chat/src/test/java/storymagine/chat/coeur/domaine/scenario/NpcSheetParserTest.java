package storymagine.chat.coeur.domaine.scenario;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcSheetParserTest {

    private static final String SHEET = """
        # Léa
        Léa, vingt-six ans, réservée.
        # Apparence
        Cheveux courts.

        ## froide  distante au début, à retirer après la forêt
        Distante avec Marc : réponses brèves.

        ## amicale
        Souriante avec Marc.

        # SECRET
        Elle déteste se tromper sur les gens.

        ## soupcon
        Elle soupçonne Marc d'un trafic.
        """;

    @Test
    void aSheetWithoutTagsReadsExactlyAsBefore() {
        Npc npc = NpcSheetParser.parse("lea", "Léa", "# Léa\nUne marchande.\n# Apparence\nGrande.\n# SECRET\nUne dette.\n# Plan\nFuir.");

        assertEquals("# Léa\nUne marchande.\n# Apparence\nGrande.", npc.publicInfo());
        assertEquals("Une dette.\n# Plan\nFuir.", npc.secretInfo());
        assertTrue(npc.tags().isEmpty());
        assertEquals("# Léa\nUne marchande.", NpcSheetParser.parse("x", "", "# Léa\nUne marchande.").publicInfo());
        assertEquals("", NpcSheetParser.parse("x", "", "# Léa\nUne marchande.").secretInfo());
    }

    @Test
    void tagsBeforeSecretArePublicAndTagsAfterItAreSecret() {
        Npc npc = NpcSheetParser.parse("lea", "Léa", SHEET);

        assertEquals(List.of("froide", "amicale", "soupcon"), npc.tags().stream().map(StoryTag::name).toList());
        assertEquals(CharacterDelta.Visibility.PUBLIC, npc.tags().get(0).visibility());
        assertEquals(CharacterDelta.Visibility.PUBLIC, npc.tags().get(1).visibility());
        assertEquals(CharacterDelta.Visibility.SECRET, npc.tags().get(2).visibility());
    }

    @Test
    void theTagIsTheFirstWordOfTheHeadingAndTheRestOfTheLineIsNeverSent() {
        Npc npc = NpcSheetParser.parse("lea", "Léa", SHEET);

        StoryTag froide = npc.tags().get(0);
        assertEquals("Distante avec Marc : réponses brèves.", froide.text());
        assertTrue(npc.hasTag("FROIDE"));
        assertTrue(!npc.fullSheet().contains("distante au début"), npc.fullSheet());
        assertTrue(!npc.fullSheet().contains("##"), npc.fullSheet());
    }

    @Test
    void thePermanentTextOfEachZoneStaysOutsideTheTagsAndASingleHashIsPlainText() {
        Npc npc = NpcSheetParser.parse("lea", "Léa", SHEET);

        assertEquals("# Léa\nLéa, vingt-six ans, réservée.\n# Apparence\nCheveux courts.", npc.publicInfo());
        assertEquals("Elle déteste se tromper sur les gens.", npc.secretInfo());
    }

    @Test
    void aBrokenTagHeadingFailsWithTheSheetNamed() {
        assertThrows(IllegalArgumentException.class, () -> NpcSheetParser.parse("lea", "", "# Léa\n##\ntexte"));
        IllegalArgumentException reserved = assertThrows(IllegalArgumentException.class,
            () -> NpcSheetParser.parse("lea", "", "# Léa\n## secret\ntexte"));
        assertTrue(reserved.getMessage().contains("SECRET"), reserved.getMessage());
        IllegalArgumentException empty = assertThrows(IllegalArgumentException.class,
            () -> NpcSheetParser.parse("lea", "", "# Léa\n## vide\n\n## autre\ntexte"));
        assertTrue(empty.getMessage().contains("lea") && empty.getMessage().contains("vide"), empty.getMessage());
    }

    @Test
    void aTagNameIsUniqueInASheetWhateverTheCaseAndTheZone() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> NpcSheetParser.parse("lea", "", "# Léa\n## dette\npublic\n# SECRET\n## DETTE\nsecret"));

        assertTrue(e.getMessage().contains("existe deja"), e.getMessage());
    }
}
