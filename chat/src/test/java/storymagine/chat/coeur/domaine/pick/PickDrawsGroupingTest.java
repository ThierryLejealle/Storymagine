package storymagine.chat.coeur.domaine.pick;

import org.junit.jupiter.api.Test;
import storymagine.chat.coeur.domaine.scenario.Cast;
import storymagine.chat.coeur.domaine.scenario.Npc;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The draws of a run are grouped by copy of a "{repeat}" block, so the 🎲 table can show each iteration on its own. */
class PickDrawsGroupingTest {

    private static final Cast THREE = new Cast(List.of(
        new Npc("jack", "Jack", "", ""), new Npc("mia", "Mia", "", ""), new Npc("john", "John", "", "")));
    private static final PickLibrary LIBRARY = tag -> PickParagraphs.parse(
        Map.of("place", "Harbor.\n---\nMarket.", "trial", "Bridge.\n-\nTruth.\n---\nRiddle.\n-\nAnswer.").get(tag.name()));

    private static List<String> describe(String text) {
        return new PickResolver(LIBRARY, bound -> 0, THREE).resolve(List.of(text)).draws().values().stream()
            .map(d -> d.mode().label() + " " + d.tag() + " " + (d.scope().isNone() ? "-" : d.scope().token()))
            .toList();
    }

    @Test
    void eachCopyGroupsItsItemItsPicksAndItsCastsInThatOrder() {
        List<String> described = describe("{pick random place}\n"
            + "{repeat order cast CHEF}\n{cast PARTNER}\n{pick random place}\n%CHEF%/%PARTNER%\n{end-repeat}\n");

        assertEquals(List.of(
            "random place -",
            "repeat-order CHEF 1:CHEF:1/3", "random place 1:CHEF:1/3", "cast PARTNER 1:CHEF:1/3",
            "repeat-order CHEF 1:CHEF:2/3", "random place 1:CHEF:2/3", "cast PARTNER 1:CHEF:2/3",
            "repeat-order CHEF 1:CHEF:3/3", "random place 1:CHEF:3/3", "cast PARTNER 1:CHEF:3/3"), described);
    }

    @Test
    void aPickBeforeBetweenAndAfterTheBlocksKeepsItsPlaceAndTwoBlocksNeverShareAScope() {
        List<String> described = describe("{pick random place}\n{repeat 2}\n{pick random place}\n{end-repeat}\n"
            + "{pick random place}\n{repeat 2}\n{pick random place}\n{end-repeat}\n{pick random place}");

        assertEquals(List.of(
            "random place -",
            "random place 1:repeat:1/2", "random place 1:repeat:2/2",
            "random place -",
            "random place 2:repeat:1/2", "random place 2:repeat:2/2",
            "random place -"), described);
    }

    @Test
    void theItemOfACopyKeepsItsPiecesInOneDrawSoTheTableCanNameThem() {
        PickResolution resolution = new PickResolver(LIBRARY, bound -> 0, THREE)
            .resolve(List.of("{repeat order trial}\n{item1} / {item2}\n{end-repeat}"));

        List<PickDraw> draws = resolution.draws().values();
        assertEquals(2, draws.size());
        assertEquals(List.of("Bridge.\n-\nTruth."), draws.get(0).paragraphs());
        assertEquals(new PickScope(1, "trial", 2, 2), draws.get(1).scope());
    }

    @Test
    void aRunDrawnWithoutAnyRepeatKeepsItsPicksInTextOrder() {
        assertEquals(List.of("random place -", "unique place -"), describe("{pick random place}\n{pick unique place}"));
    }
}
