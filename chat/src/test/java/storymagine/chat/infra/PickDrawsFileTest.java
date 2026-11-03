package storymagine.chat.infra;

import org.junit.jupiter.api.Test;
import storymagine.chat.coeur.domaine.pick.PickDraw;
import storymagine.chat.coeur.domaine.pick.PickDraws;
import storymagine.chat.coeur.domaine.pick.PickMode;
import storymagine.chat.coeur.domaine.pick.PickScope;
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
    void theCopyOfARepeatBlockATirageWasMadeInSurvivesTheFile() {
        PickScope scope = new PickScope(2, "defis", 3, 5);
        PickDraws draws = new PickDraws(List.of(
            new PickDraw(PickMode.REPEAT_RANDOM, new PickTag("defis"), List.of("Pont.\n-\nUne verite."), scope),
            new PickDraw(PickMode.RANDOM, new PickTag("/lib/evenements"), List.of("Un orage."), scope),
            new PickDraw(PickMode.RANDOM, new PickTag("mood"), List.of("Gloomy."))));

        String file = PickDrawsFile.format(draws);

        assertTrue(file.contains("@@ repeat-random defis 2:defis:3/5"), file);
        assertEquals(draws, PickDrawsFile.parse(file));
    }

    @Test
    void aFileWrittenBeforeScopesStillReadsAsDrawsOutsideAnyCopy() {
        PickDraws draws = PickDrawsFile.parse("@@ repeat-order plain\nAlpha.\n@@ --\nBeta.\n@@ random place\nHarbor.\n");

        assertEquals(2, draws.values().size());
        assertTrue(draws.values().get(0).scope().isNone());
        assertEquals(List.of("Alpha.", "Beta."), draws.values().get(0).paragraphs());
    }

    @Test
    void emptyDrawsGiveAnEmptyFileAndBack() {
        assertEquals("", PickDrawsFile.format(PickDraws.NONE));
        assertTrue(PickDrawsFile.parse("").isEmpty());
    }
}
