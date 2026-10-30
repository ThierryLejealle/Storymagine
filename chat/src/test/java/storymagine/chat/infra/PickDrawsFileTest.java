package storymagine.chat.infra;

import org.junit.jupiter.api.Test;
import storymagine.chat.coeur.domaine.pick.PickDraw;
import storymagine.chat.coeur.domaine.pick.PickDraws;
import storymagine.chat.coeur.domaine.pick.PickMode;
import storymagine.chat.coeur.domaine.pick.PickTag;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PickDrawsFileTest {

    @Test
    void roundTripKeepsOrderModeTagAndMultiParagraphDraws() {
        PickDraws draws = new PickDraws(List.of(
            new PickDraw(PickMode.UNIQUE, new PickTag("place"), List.of("Foggy harbor.\nGulls overhead.")),
            new PickDraw(PickMode.RANDOM, new PickTag("mood"), List.of("Gloomy.")),
            new PickDraw(PickMode.ofCount(2), new PickTag("rumor"), List.of("The mayor vanished.", "A boat came back empty.")),
            new PickDraw(PickMode.ALL, new PickTag("rules"), List.of("No weapons.", "Curfew at midnight."))));

        assertEquals(draws, PickDrawsFile.parse(PickDrawsFile.format(draws)));
    }

    @Test
    void emptyDrawsGiveAnEmptyFileAndBack() {
        assertEquals("", PickDrawsFile.format(PickDraws.NONE));
        assertTrue(PickDrawsFile.parse("").isEmpty());
    }
}
