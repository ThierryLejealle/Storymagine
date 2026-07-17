package storymagine.chat.coeur.domaine.session;

import org.junit.jupiter.api.Test;
import storymagine.chat.coeur.domaine.scenario.ActNumber;
import storymagine.chat.coeur.domaine.scenario.Cast;
import storymagine.chat.coeur.domaine.scenario.ChatScenario;
import storymagine.chat.coeur.domaine.scenario.Npc;
import storymagine.chat.coeur.domaine.scenario.ScenarioAct;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatSessionTest {

    private static ChatScenario scenario(String premise, List<ScenarioAct> acts) {
        Cast cast = new Cast(List.of(new Npc("test-npc", "", "sheet", "")));
        return new ChatScenario("test", cast, premise, acts);
    }

    @Test
    void freshWithActsOpensWithFirstActsNarratorLines() {
        ChatScenario scenario = scenario("premise", List.of(
            ScenarioAct.leaf(ActNumber.of(1), "", "Act one title\n[The door creaks open.]\n\nBody text..."),
            ScenarioAct.leaf(ActNumber.of(2), "", "Act two title\n\nBody text...")));

        ChatSession session = ChatSession.fresh(scenario);

        assertEquals(1, session.currentAct());
        assertEquals(List.of(new ChatTurn(ChatTurn.Speaker.NARRATOR, "The door creaks open.")), session.turns());
    }

    @Test
    void freshWithoutActsOpensWithThePremisesNarratorLines() {
        ChatScenario scenario = scenario("Premise title\n[A storm is brewing.]\n\nMore premise text.", List.of());

        ChatSession session = ChatSession.fresh(scenario);

        assertEquals(0, session.currentAct());
        assertEquals(List.of(new ChatTurn(ChatTurn.Speaker.NARRATOR, "A storm is brewing.")), session.turns());
    }

    @Test
    void freshWithoutActsOnlyUsesTheFirstBracketedLineEvenIfThePremiseHasSeveral() {
        // Un scenario sans actes n'a qu'UN SEUL etat d'ouverture — plusieurs blocs "[...]" dans une
        // premisse plate (plusieurs scenes narratives pensees pour s'enchainer, pas forcement comme
        // des actes formels) ne doivent jamais etre tous deverses d'un coup avant le premier message.
        ChatScenario scenario = scenario(
            "Premise title\n[A storm is brewing.]\n\nMore text.\n[They reach the door.]\n\n[It creaks open.]",
            List.of());

        ChatSession session = ChatSession.fresh(scenario);

        assertEquals(List.of(new ChatTurn(ChatTurn.Speaker.NARRATOR, "A storm is brewing.")), session.turns());
    }

    @Test
    void freshWithNoBracketedLinesOpensWithNoTurns() {
        ChatScenario scenario = scenario("premise", List.of(
            ScenarioAct.leaf(ActNumber.of(1), "", "Act one, no story beats.")));

        ChatSession session = ChatSession.fresh(scenario);

        assertTrue(session.turns().isEmpty());
    }

    @Test
    void advanceActAppendsTheNewActsNarratorLinesAfterExistingTurns() {
        ChatScenario scenario = scenario("premise", List.of(
            ScenarioAct.leaf(ActNumber.of(1), "", "Act one."),
            ScenarioAct.leaf(ActNumber.of(2), "", "Act two.\n[They found the map.]\n\nBody.\n[They decide to rest first.]")));
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
        ChatScenario scenario = scenario("premise", List.of(
            ScenarioAct.leaf(ActNumber.of(1), "", "Act one."),
            ScenarioAct.leaf(ActNumber.of(2), "", "Act two.")));
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
        ChatScenario scenario = scenario("premise", List.of(
            ScenarioAct.leaf(ActNumber.of(1), "", "Act one."),
            ScenarioAct.leaf(ActNumber.of(2), "", "Act two.")));
        ChatSession session = ChatSession.fresh(scenario);

        boolean movedBack = session.previousAct();

        assertFalse(movedBack);
        assertEquals(1, session.currentAct());
    }

    @Test
    void restoreReplacesTurnsSummaryCurrentActAndPresenceInPlace() {
        ChatScenario scenario = scenario("premise", List.of(
            ScenarioAct.leaf(ActNumber.of(1), "", "Act one."),
            ScenarioAct.leaf(ActNumber.of(2), "", "Act two.")));
        ChatSession session = ChatSession.fresh(scenario);
        session.append(new ChatTurn(ChatTurn.Speaker.PLAYER, "Current turn, about to be discarded."));

        session.restore("Old summary.", List.of(new ChatTurn(ChatTurn.Speaker.PLAYER, "Restored turn.")), 2,
            Set.of("test-npc"), Set.of("test-npc"));

        assertEquals("Old summary.", session.summary());
        assertEquals(List.of(new ChatTurn(ChatTurn.Speaker.PLAYER, "Restored turn.")), session.turns());
        assertEquals(2, session.currentAct());
        assertEquals(Set.of("test-npc"), session.presentNpcIds());
        assertEquals(Set.of("test-npc"), session.interjectingNpcIds());
    }

    @Test
    void freshSessionStartsWithEveryCastMemberPresent() {
        Cast cast = new Cast(List.of(new Npc("elena", "Elena", "sheet", ""), new Npc("marcus", "Marcus", "sheet", "")));
        ChatScenario scenario = new ChatScenario("test", cast, "premise", List.of());

        ChatSession session = ChatSession.fresh(scenario);

        assertEquals(Set.of("elena", "marcus"), session.presentNpcIds());
    }

    @Test
    void setPresentMutesAndUnmutesButNeverTheLastPresentNpc() {
        Cast cast = new Cast(List.of(new Npc("elena", "Elena", "sheet", ""), new Npc("marcus", "Marcus", "sheet", "")));
        ChatSession session = ChatSession.fresh(new ChatScenario("test", cast, "premise", List.of()));

        assertTrue(session.setPresent("marcus", false));
        assertEquals(Set.of("elena"), session.presentNpcIds());

        assertFalse(session.setPresent("elena", false), "impossible de retirer le dernier present");
        assertEquals(Set.of("elena"), session.presentNpcIds());

        assertTrue(session.setPresent("marcus", true));
        assertEquals(Set.of("elena", "marcus"), session.presentNpcIds());
    }

    @Test
    void reloadScenarioReplacesScenarioAndKeepsTurnsSummaryAndCurrentAct() {
        Cast cast = new Cast(List.of(new Npc("elena", "Elena", "old sheet", "")));
        ChatSession session = ChatSession.fresh(new ChatScenario("test", cast, "premise", List.of()));
        session.append(new ChatTurn(ChatTurn.Speaker.PLAYER, "Hi."));

        Cast freshCast = new Cast(List.of(new Npc("elena", "Elena", "new sheet", "")));
        ChatScenario freshScenario = new ChatScenario("test", freshCast, "premise", List.of());
        session.reloadScenario(freshScenario);

        assertEquals("new sheet", session.scenario().cast().find("elena").orElseThrow().publicInfo());
        assertEquals(List.of(new ChatTurn(ChatTurn.Speaker.PLAYER, "Hi.")), session.turns());
    }

    @Test
    void reloadScenarioDropsRemovedNpcsKeepsExistingPresenceAndAddsNewNpcsAsPresent() {
        Cast cast = new Cast(List.of(new Npc("elena", "Elena", "sheet", ""), new Npc("marcus", "Marcus", "sheet", "")));
        ChatSession session = ChatSession.fresh(new ChatScenario("test", cast, "premise", List.of()));
        session.setPresent("marcus", false); // elena present, marcus muted

        Cast freshCast = new Cast(List.of(new Npc("elena", "Elena", "sheet", ""), new Npc("clara", "Clara", "sheet", "")));
        session.reloadScenario(new ChatScenario("test", freshCast, "premise", List.of()));

        assertEquals(Set.of("elena", "clara"), session.presentNpcIds(), "marcus disparu, clara nouvelle donc presente");
    }

    @Test
    void reloadScenarioFallsBackToEveryNewNpcPresentIfReconciliationWouldLeaveNobody() {
        Cast cast = new Cast(List.of(new Npc("elena", "Elena", "sheet", "")));
        ChatSession session = ChatSession.fresh(new ChatScenario("test", cast, "premise", List.of()));

        Cast freshCast = new Cast(List.of(new Npc("elena-renamed", "Elena", "sheet", "")));
        session.reloadScenario(new ChatScenario("test", freshCast, "premise", List.of()));

        assertEquals(Set.of("elena-renamed"), session.presentNpcIds());
    }

    // ── Interjection eligibility ─────────────────────────────────────────────

    @Test
    void freshSessionStartsWithEveryCastMemberInterjectionEligible() {
        Cast cast = new Cast(List.of(new Npc("elena", "Elena", "sheet", ""), new Npc("marcus", "Marcus", "sheet", "")));
        ChatScenario scenario = new ChatScenario("test", cast, "premise", List.of());

        ChatSession session = ChatSession.fresh(scenario);

        assertEquals(Set.of("elena", "marcus"), session.interjectingNpcIds());
    }

    @Test
    void setInterjectingOptsInAndOutWithNoMinimumGuard() {
        Cast cast = new Cast(List.of(new Npc("elena", "Elena", "sheet", ""), new Npc("marcus", "Marcus", "sheet", "")));
        ChatSession session = ChatSession.fresh(new ChatScenario("test", cast, "premise", List.of()));

        assertTrue(session.setInterjecting("marcus", false));
        assertEquals(Set.of("elena"), session.interjectingNpcIds());

        // contrairement a setPresent, opter tout le monde dehors est un no-op valide, pas bloque
        assertTrue(session.setInterjecting("elena", false));
        assertEquals(Set.of(), session.interjectingNpcIds());

        assertFalse(session.setInterjecting("elena", false), "deja hors, rien ne change");

        assertTrue(session.setInterjecting("marcus", true));
        assertEquals(Set.of("marcus"), session.interjectingNpcIds());
    }

    @Test
    void reloadScenarioReconcilesInterjectionEligibilityLikePresence() {
        Cast cast = new Cast(List.of(new Npc("elena", "Elena", "sheet", ""), new Npc("marcus", "Marcus", "sheet", "")));
        ChatSession session = ChatSession.fresh(new ChatScenario("test", cast, "premise", List.of()));
        session.setInterjecting("marcus", false); // elena eligible, marcus opte dehors

        Cast freshCast = new Cast(List.of(new Npc("elena", "Elena", "sheet", ""), new Npc("clara", "Clara", "sheet", "")));
        session.reloadScenario(new ChatScenario("test", freshCast, "premise", List.of()));

        assertEquals(Set.of("elena", "clara"), session.interjectingNpcIds(),
            "marcus disparu, clara nouvelle donc eligible par defaut");
    }
}
