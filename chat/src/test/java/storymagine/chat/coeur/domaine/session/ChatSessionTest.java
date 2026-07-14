package storymagine.chat.coeur.domaine.session;

import org.junit.jupiter.api.Test;
import storymagine.chat.coeur.domaine.scenario.ActNumber;
import storymagine.chat.coeur.domaine.scenario.ChatScenario;
import storymagine.chat.coeur.domaine.scenario.ScenarioAct;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatSessionTest {

    @Test
    void freshWithActsOpensWithFirstActsNarratorLines() {
        ChatScenario scenario = new ChatScenario("test", "sheet", "premise", List.of(
            ScenarioAct.leaf(ActNumber.of(1), "", "Act one title\n[The door creaks open.]\n\nBody text..."),
            ScenarioAct.leaf(ActNumber.of(2), "", "Act two title\n\nBody text...")), "");

        ChatSession session = ChatSession.fresh(scenario);

        assertEquals(1, session.currentAct());
        assertEquals(List.of(new ChatTurn(ChatTurn.Speaker.NARRATOR, "The door creaks open.")), session.turns());
    }

    @Test
    void freshWithoutActsOpensWithThePremisesNarratorLines() {
        ChatScenario scenario = new ChatScenario("test", "sheet",
            "Premise title\n[A storm is brewing.]\n\nMore premise text.", List.of(), "");

        ChatSession session = ChatSession.fresh(scenario);

        assertEquals(0, session.currentAct());
        assertEquals(List.of(new ChatTurn(ChatTurn.Speaker.NARRATOR, "A storm is brewing.")), session.turns());
    }

    @Test
    void freshWithNoBracketedLinesOpensWithNoTurns() {
        ChatScenario scenario = new ChatScenario("test", "sheet", "premise", List.of(
            ScenarioAct.leaf(ActNumber.of(1), "", "Act one, no story beats.")), "");

        ChatSession session = ChatSession.fresh(scenario);

        assertTrue(session.turns().isEmpty());
    }

    @Test
    void advanceActAppendsTheNewActsNarratorLinesAfterExistingTurns() {
        ChatScenario scenario = new ChatScenario("test", "sheet", "premise", List.of(
            ScenarioAct.leaf(ActNumber.of(1), "", "Act one."),
            ScenarioAct.leaf(ActNumber.of(2), "", "Act two.\n[They found the map.]\n\nBody.\n[They decide to rest first.]")), "");
        ChatSession session = ChatSession.fresh(scenario);
        session.append(new ChatTurn(ChatTurn.Speaker.PLAYER, "Hi."));
        session.append(new ChatTurn(ChatTurn.Speaker.LLM, "Hello."));

        boolean advanced = session.advanceAct();

        assertTrue(advanced);
        assertEquals(List.of(
            new ChatTurn(ChatTurn.Speaker.PLAYER, "Hi."),
            new ChatTurn(ChatTurn.Speaker.LLM, "Hello."),
            new ChatTurn(ChatTurn.Speaker.NARRATOR, "They found the map."),
            new ChatTurn(ChatTurn.Speaker.NARRATOR, "They decide to rest first.")
        ), session.turns());
    }

    @Test
    void previousActMovesBackOneActWithoutTouchingTurns() {
        ChatScenario scenario = new ChatScenario("test", "sheet", "premise", List.of(
            ScenarioAct.leaf(ActNumber.of(1), "", "Act one."),
            ScenarioAct.leaf(ActNumber.of(2), "", "Act two.")), "");
        ChatSession session = ChatSession.fresh(scenario);
        session.advanceAct();
        List<ChatTurn> turnsBefore = session.turns();

        boolean movedBack = session.previousAct();

        assertTrue(movedBack);
        assertEquals(1, session.currentAct());
        assertEquals(turnsBefore, session.turns());
    }

    @Test
    void previousActIsANoOpOnTheFirstAct() {
        ChatScenario scenario = new ChatScenario("test", "sheet", "premise", List.of(
            ScenarioAct.leaf(ActNumber.of(1), "", "Act one."),
            ScenarioAct.leaf(ActNumber.of(2), "", "Act two.")), "");
        ChatSession session = ChatSession.fresh(scenario);

        boolean movedBack = session.previousAct();

        assertFalse(movedBack);
        assertEquals(1, session.currentAct());
    }

    @Test
    void restoreReplacesTurnsSummaryAndCurrentActInPlace() {
        ChatScenario scenario = new ChatScenario("test", "sheet", "premise", List.of(
            ScenarioAct.leaf(ActNumber.of(1), "", "Act one."),
            ScenarioAct.leaf(ActNumber.of(2), "", "Act two.")), "");
        ChatSession session = ChatSession.fresh(scenario);
        session.append(new ChatTurn(ChatTurn.Speaker.PLAYER, "Current turn, about to be discarded."));

        session.restore("Old summary.", List.of(new ChatTurn(ChatTurn.Speaker.PLAYER, "Restored turn.")), 2);

        assertEquals("Old summary.", session.summary());
        assertEquals(List.of(new ChatTurn(ChatTurn.Speaker.PLAYER, "Restored turn.")), session.turns());
        assertEquals(2, session.currentAct());
    }
}
