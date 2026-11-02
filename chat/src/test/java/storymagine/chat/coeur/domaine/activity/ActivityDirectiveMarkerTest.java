package storymagine.chat.coeur.domaine.activity;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActivityDirectiveMarkerTest {

    @Test
    void extractsEveryCommandLineAndCutsThemOutOfTheText() {
        ActivityDirectiveMarker.Extracted extracted = ActivityDirectiveMarker.extract("""
            Elena sert un verre.
            {npc-active Elena Marcus}
            {npc-passive}
            {npc-reset}
            Marcus observe.
            """);

        assertEquals(List.of(
            ActivityDirective.level(NpcActivity.ACTIVE, "Elena Marcus"),
            ActivityDirective.level(NpcActivity.PASSIVE, ""),
            ActivityDirective.reset()), extracted.directives());
        assertEquals("Elena sert un verre.\nMarcus observe.\n", extracted.text());
    }

    @Test
    void aStarCommandIsExtractedAsTheAllDirective() {
        ActivityDirectiveMarker.Extracted extracted = ActivityDirectiveMarker.extract("Texte.\n{npc-active *}\nSuite.\n");

        assertEquals("Texte.\nSuite.\n", extracted.text());
        assertEquals(1, extracted.directives().size());
        assertTrue(extracted.directives().get(0).isAll());
        assertEquals(NpcActivity.ACTIVE, extracted.directives().get(0).level());
    }

    @Test
    void theKeywordIsCaseInsensitiveAndTheNamesAreKeptAsWritten() {
        ActivityDirective directive = ActivityDirectiveMarker.extract("{NPC-Absent  Céleste Dubois }\n").directives().get(0);

        assertEquals(NpcActivity.ABSENT, directive.level());
        assertEquals("Céleste Dubois", directive.namesText());
    }

    @Test
    void aTextWithoutCommandIsReturnedUntouched() {
        ActivityDirectiveMarker.Extracted extracted = ActivityDirectiveMarker.extract("Rien ici.\n{npc-starts Elena}\n");

        assertEquals(List.of(), extracted.directives());
        assertEquals("Rien ici.\n{npc-starts Elena}\n", extracted.text());
    }

    @Test
    void aCommandInTheMiddleOfASentenceIsAnErrorNeverSilentlyKept() {
        assertThrows(IllegalArgumentException.class, () -> ActivityDirectiveMarker.extract("Elena {npc-active Marcus} entre.\n"));
    }

    @Test
    void aResetWithNamesIsAnError() {
        assertThrows(IllegalArgumentException.class, () -> ActivityDirectiveMarker.extract("{npc-reset Elena}\n"));
    }

    @Test
    void modeLinesAreReadInTheOrderWrittenAndCutOut() {
        ActivityDirectiveMarker.Extracted extracted = ActivityDirectiveMarker.extract(
            "Texte.\n{npc-mode do on Elena}\n{NPC-MODE DO off *}\nSuite.\n");

        assertEquals(List.of(ActivityDirective.modeOn(NpcMode.DO, "Elena"), ActivityDirective.modeOff(NpcMode.DO, "*")),
            extracted.directives());
        assertEquals("Texte.\nSuite.\n", extracted.text());
    }

    @Test
    void anUnknownModeOrAModeWithoutNamesIsAnErrorNeverLeftInTheText() {
        IllegalArgumentException unknown = assertThrows(IllegalArgumentException.class,
            () -> ActivityDirectiveMarker.extract("{npc-mode fly on Elena}\n"));
        assertTrue(unknown.getMessage().contains("fly") && unknown.getMessage().contains("do"), unknown.getMessage());
        assertThrows(IllegalArgumentException.class, () -> ActivityDirectiveMarker.extract("{npc-mode do on}\n"));
        assertThrows(IllegalArgumentException.class, () -> ActivityDirectiveMarker.extract("Elena {npc-mode do on Elena} ici\n"));
    }

    @Test
    void theNpcDoOpeningCommandIsNotAnActivityCommand() {
        assertEquals(List.of(), ActivityDirectiveMarker.extract("{npc-do Elena}\n").directives());
    }
}
