package storymagine.chat.coeur.domaine.session;

import org.junit.jupiter.api.Test;
import storymagine.chat.coeur.domaine.activity.ActivityDirective;
import storymagine.chat.coeur.domaine.activity.ActivitySource;
import storymagine.chat.coeur.domaine.activity.ForcedActivity;
import storymagine.chat.coeur.domaine.activity.NpcActivity;
import storymagine.chat.coeur.domaine.activity.NpcActivityState;
import storymagine.chat.coeur.domaine.scenario.ActNumber;
import storymagine.chat.coeur.domaine.scenario.Cast;
import storymagine.chat.coeur.domaine.scenario.ChatScenario;
import storymagine.chat.coeur.domaine.scenario.Npc;
import storymagine.chat.coeur.domaine.scenario.NpcOpening;
import storymagine.chat.coeur.domaine.scenario.ScenarioAct;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

        session.restore("Old summary.", List.of(new ChatTurn(ChatTurn.Speaker.PLAYER, "Restored turn.")), 2, Map.of());

        assertEquals("Old summary.", session.summary());
        assertEquals(List.of(new ChatTurn(ChatTurn.Speaker.PLAYER, "Restored turn.")), session.turns());
        assertEquals(2, session.currentAct());
        assertEquals(Set.of("test-npc"), session.presentNpcIds());
    }

    // ── Numerotation des tours ──────────────────────────────────────────────

    private static ChatSession sessionWithSixTurns() {
        ChatSession session = ChatSession.fresh(scenario("premise", List.of()));
        for (int i = 1; i <= 6; i++) {
            session.append(new ChatTurn(i % 2 == 1 ? ChatTurn.Speaker.PLAYER : ChatTurn.Speaker.LLM, "turn " + i));
        }
        return session;
    }

    @Test
    void turnsAreNumberedFromOneAndEachCarriesTheActItWasWrittenIn() {
        ChatScenario scenario = scenario("premise", List.of(
            ScenarioAct.leaf(ActNumber.of(1), "", "Act one."),
            ScenarioAct.leaf(ActNumber.of(2), "", "Act two.")));
        ChatSession session = ChatSession.fresh(scenario);
        session.append(new ChatTurn(ChatTurn.Speaker.PLAYER, "First."));
        session.advanceAct();
        session.append(new ChatTurn(ChatTurn.Speaker.PLAYER, "Second."));

        List<NumberedTurn> numbered = session.numberedTurns();

        assertEquals(List.of(TurnNumber.of(1), TurnNumber.of(2)), numbered.stream().map(NumberedTurn::number).toList());
        assertEquals(List.of(1, 2), numbered.stream().map(NumberedTurn::act).toList());
        assertEquals(TurnNumber.of(1), session.firstTurnNumber());
        assertEquals(TurnNumber.of(2), session.lastTurnNumber());
    }

    @Test
    void anEmptySessionHasNoLastTurn() {
        ChatSession session = ChatSession.fresh(scenario("premise", List.of()));

        assertEquals(TurnNumber.NONE, session.lastTurnNumber());
    }

    @Test
    void narratorBeatsOfAnActAdvanceAreNumberedAndStampedWithTheNewAct() {
        ChatScenario scenario = scenario("premise", List.of(
            ScenarioAct.leaf(ActNumber.of(1), "", "Act one."),
            ScenarioAct.leaf(ActNumber.of(2), "", "Act two.\n[The storm breaks.]")));
        ChatSession session = ChatSession.fresh(scenario);
        session.append(new ChatTurn(ChatTurn.Speaker.LLM, "Time to go. [NEXT ACT]"));

        session.advanceAct();

        List<NumberedTurn> numbered = session.numberedTurns();
        assertEquals(2, numbered.size());
        assertEquals(1, numbered.get(0).act(), "la reponse qui declenche l'avance garde l'ancien acte");
        assertEquals(2, numbered.get(1).act(), "les [...] du nouvel acte portent le nouvel acte");
        assertEquals(ChatTurn.Speaker.NARRATOR, numbered.get(1).turn().speaker());
    }

    @Test
    void truncateFromFreesTheNumbersOfTheDroppedTurns() {
        ChatSession session = sessionWithSixTurns();

        session.truncateFrom(2);
        session.append(new ChatTurn(ChatTurn.Speaker.PLAYER, "again"));

        assertEquals(TurnNumber.of(3), session.lastTurnNumber());
        assertEquals("again", session.numberedTurns().get(2).turn().text());
    }

    @Test
    void compactIntoDropsTheHeadKeepsNumbersStableAndReturnsTheCompaction() {
        ChatSession session = sessionWithSixTurns();

        Compaction compaction = session.compactInto("Folded.", session.turns().subList(4, 6));

        assertEquals(new Compaction(TurnNumber.of(6), TurnNumber.of(4), "Folded."), compaction);
        assertEquals("Folded.", session.summary());
        assertEquals(TurnNumber.of(5), session.firstTurnNumber());
        assertEquals(TurnNumber.of(6), session.lastTurnNumber());
        assertEquals(List.of(TurnNumber.of(5), TurnNumber.of(6)),
            session.numberedTurns().stream().map(NumberedTurn::number).toList(), "un tour garde son numero apres compaction");
    }

    @Test
    void numbersKeepGrowingAfterACompaction() {
        ChatSession session = sessionWithSixTurns();
        session.compactInto("Folded.", session.turns().subList(4, 6));

        session.append(new ChatTurn(ChatTurn.Speaker.PLAYER, "seven"));

        assertEquals(TurnNumber.of(7), session.lastTurnNumber());
    }

    @Test
    void aCompactionCanFoldEverythingLeavingAnEmptyWindowThatStillKnowsWhereItStands() {
        ChatSession session = sessionWithSixTurns();

        Compaction compaction = session.compactInto("All folded.", List.of());

        assertEquals(new Compaction(TurnNumber.of(6), TurnNumber.of(6), "All folded."), compaction);
        assertTrue(session.turns().isEmpty());
        assertEquals(TurnNumber.of(6), session.lastTurnNumber());
    }

    @Test
    void restoredRejectsTurnsThatDoNotStartAtTheGivenFirstNumber() {
        ChatScenario scenario = scenario("premise", List.of());
        List<NumberedTurn> turns = List.of(
            new NumberedTurn(TurnNumber.of(5), 0, new ChatTurn(ChatTurn.Speaker.PLAYER, "a")),
            new NumberedTurn(TurnNumber.of(7), 0, new ChatTurn(ChatTurn.Speaker.PLAYER, "gap")));

        assertThrows(IllegalStateException.class, () -> ChatSession.restored(scenario, "", TurnNumber.of(5), turns, 0,
            Map.of()));
    }

    @Test
    void restoreConversationFromReplacesTurnsSummaryAndActButNeverTheForcedLevels() {
        ChatScenario scenario = scenario("premise", List.of(
            ScenarioAct.leaf(ActNumber.of(1), "", "Act one."),
            ScenarioAct.leaf(ActNumber.of(2), "", "Act two.")));
        ChatSession live = new ChatSession(scenario, List.of(new ChatTurn(ChatTurn.Speaker.PLAYER, "now")), "new summary", 2,
            Map.of("test-npc", new ForcedActivity(NpcActivity.PASSIVE, "")));
        ChatSession earlier = ChatSession.restored(scenario, "old summary", TurnNumber.of(1),
            List.of(new NumberedTurn(TurnNumber.of(1), 1, new ChatTurn(ChatTurn.Speaker.PLAYER, "then"))), 1, Map.of());

        live.restoreConversationFrom(earlier);

        assertEquals("old summary", live.summary());
        assertEquals(1, live.currentAct());
        assertEquals("then", live.turns().get(0).text());
        assertEquals(NpcActivity.PASSIVE, live.activity().levelOf("test-npc"), "les reglages du joueur ne sont pas rembobines");
    }

    @Test
    void restoreConversationFromKeepsTheInMemoryTurnWhenItIsTheSameSoItsThinkingSurvives() {
        ChatScenario scenario = scenario("premise", List.of());
        ChatTurn withThinking = new ChatTurn(ChatTurn.Speaker.LLM, "reply", "my private reasoning", "test-npc");
        ChatSession live = new ChatSession(scenario, List.of(new ChatTurn(ChatTurn.Speaker.PLAYER, "hi"), withThinking), "", 0);
        ChatSession reloaded = ChatSession.restored(scenario, "", TurnNumber.of(1), List.of(
            new NumberedTurn(TurnNumber.of(1), 0, new ChatTurn(ChatTurn.Speaker.PLAYER, "hi")),
            new NumberedTurn(TurnNumber.of(2), 0, new ChatTurn(ChatTurn.Speaker.LLM, "reply", "", "test-npc"))), 0,
            Map.of());

        live.restoreConversationFrom(reloaded);

        assertEquals("my private reasoning", live.turns().get(1).thinking());
    }

    @Test
    void restoreFromReplacesEverythingIncludingNumbersAndForcedLevels() {
        ChatScenario scenario = scenario("premise", List.of());
        ChatSession live = sessionWithSixTurns();
        ChatSession saved = ChatSession.restored(scenario, "saved summary", TurnNumber.of(9),
            List.of(new NumberedTurn(TurnNumber.of(9), 0, new ChatTurn(ChatTurn.Speaker.PLAYER, "nine"))), 0,
            Map.of("test-npc", new ForcedActivity(NpcActivity.PASSIVE, "")));

        live.restoreFrom(saved);

        assertEquals("saved summary", live.summary());
        assertEquals(TurnNumber.of(9), live.firstTurnNumber());
        assertEquals(TurnNumber.of(9), live.lastTurnNumber());
        assertEquals(NpcActivity.PASSIVE, live.activity().levelOf("test-npc"));
    }

    @Test
    void freshSessionStartsWithEveryCastMemberPresent() {
        Cast cast = new Cast(List.of(new Npc("elena", "Elena", "sheet", ""), new Npc("marcus", "Marcus", "sheet", "")));
        ChatScenario scenario = new ChatScenario("test", cast, "premise", List.of());

        ChatSession session = ChatSession.fresh(scenario);

        assertEquals(Set.of("elena", "marcus"), session.presentNpcIds());
    }

    // ── Niveaux d'activite ──────────────────────────────────────────────────

    private static final Cast TWO_NPCS = new Cast(List.of(new Npc("elena", "Elena", "sheet", ""), new Npc("marcus", "Marcus", "sheet", "")));

    private static ChatSession twoNpcSession() {
        return ChatSession.fresh(new ChatScenario("test", TWO_NPCS, "premise", List.of()));
    }

    /** A leaf act whose path of headings carries these commands (root first). */
    private static ScenarioAct leafWith(ActNumber number, List<List<ActivityDirective>> path) {
        return new ScenarioAct(number, "", "Act text.", List.of(), NpcOpening.NONE, List.of(), path);
    }

    @Test
    void freshSessionStartsWithEveryCastMemberPresentAndImplicit() {
        ChatSession session = twoNpcSession();

        assertEquals(Set.of("elena", "marcus"), session.presentNpcIds());
        assertEquals(NpcActivityState.DEFAULT, session.activity().stateOf("elena"));
    }

    @Test
    void aClickStepsThroughEveryForcedLevelThenBackToTheScenarioLevel() {
        ChatSession session = twoNpcSession(); // marcus : present (scenario)

        // present -> passive -> absent -> active -> present (force) -> plus de forcage
        assertTrue(session.cycleActivity("marcus"));
        assertEquals(new NpcActivityState(NpcActivity.PASSIVE, ActivitySource.FORCED), session.activity().stateOf("marcus"));
        assertTrue(session.cycleActivity("marcus"));
        assertEquals(NpcActivity.ABSENT, session.activity().levelOf("marcus"));
        assertEquals(Set.of("elena"), session.presentNpcIds());
        assertTrue(session.cycleActivity("marcus"));
        assertEquals(NpcActivity.ACTIVE, session.activity().levelOf("marcus"));
        assertTrue(session.cycleActivity("marcus"));
        assertEquals(new NpcActivityState(NpcActivity.PRESENT, ActivitySource.FORCED), session.activity().stateOf("marcus"),
            "present force n'est pas present par defaut : c'est un verrou, en rouge");
        assertTrue(session.cycleActivity("marcus"));
        assertEquals(NpcActivityState.DEFAULT, session.activity().stateOf("marcus"));
        assertTrue(session.forcedActivities().isEmpty());
    }

    @Test
    void aClickNeverLeavesTheSceneEmpty() {
        ChatSession session = ChatSession.fresh(new ChatScenario("test", new Cast(List.of(new Npc("solo", "Solo", "sheet", ""))),
            "premise", List.of()));

        // present -> passive -> (absent saute : plus personne) -> active -> present force -> retour
        session.cycleActivity("solo");
        session.cycleActivity("solo");
        assertEquals(NpcActivity.ACTIVE, session.activity().levelOf("solo"), "absent est saute quand c'est le dernier");
        assertEquals(Set.of("solo"), session.presentNpcIds());
    }

    @Test
    void cyclingAnUnknownNpcIsANoOp() {
        assertFalse(twoNpcSession().cycleActivity("nobody"));
    }

    @Test
    void theScenarioCommandsGiveTheActivityOfTheCurrentAct() {
        List<ActivityDirective> act1 = List.of(ActivityDirective.level(NpcActivity.ABSENT, "Marcus"));
        List<ActivityDirective> act2 = List.of(ActivityDirective.level(NpcActivity.ACTIVE, "Marcus"));
        ChatScenario scenario = new ChatScenario("test", TWO_NPCS, "premise", List.of(
            leafWith(ActNumber.of(1), List.of(act1)), leafWith(ActNumber.of(2), List.of(act2))));
        ChatSession session = ChatSession.fresh(scenario);

        assertEquals(Set.of("elena"), session.presentNpcIds());
        session.advanceAct();
        assertEquals(NpcActivity.ACTIVE, session.activity().levelOf("marcus"));
        session.previousAct();
        assertEquals(Set.of("elena"), session.presentNpcIds(), "calcule depuis l'acte, rien n'est stocke : le retour arriere suit");
    }

    @Test
    void aForcedLevelWinsOverTheScenarioAndIsFlaggedForced() {
        ChatScenario scenario = new ChatScenario("test", TWO_NPCS, "premise", List.of(
            leafWith(ActNumber.of(1), List.of(List.of(ActivityDirective.level(NpcActivity.ABSENT, "Marcus"))))));
        ChatSession session = ChatSession.fresh(scenario);

        session.cycleActivity("marcus"); // scenario : absent -> premier cran : active

        assertEquals(new NpcActivityState(NpcActivity.ACTIVE, ActivitySource.FORCED), session.activity().stateOf("marcus"));
        assertEquals(NpcActivity.ABSENT, session.scenarioActivity().levelOf("marcus"));
    }

    @Test
    void aForcedLevelHangsFromTheParentHeadingAndDiesWhenTheStoryLeavesIt() {
        // 1.1, 1.2 sous le titre 1 ; 2 est un autre titre racine.
        ChatScenario scenario = new ChatScenario("test", TWO_NPCS, "premise", List.of(
            ScenarioAct.leaf(ActNumber.of(1, 1), "", "a"), ScenarioAct.leaf(ActNumber.of(1, 2), "", "b"),
            ScenarioAct.leaf(ActNumber.of(2), "", "c")));
        ChatSession session = ChatSession.fresh(scenario);
        session.cycleActivity("marcus"); // force sous le titre 1

        session.advanceAct(); // 1.2 : meme parent, le forcage tient
        assertEquals(NpcActivity.PASSIVE, session.activity().levelOf("marcus"));

        session.advanceAct(); // 2 : on sort du titre 1
        assertEquals(NpcActivity.PRESENT, session.activity().levelOf("marcus"));

        session.previousAct(); // retour a 1.2 : le forcage ne ressuscite pas
        assertEquals(NpcActivity.PRESENT, session.activity().levelOf("marcus"));
        assertTrue(session.forcedActivities().isEmpty());
    }

    @Test
    void aForcedLevelOnATopLevelHeadingHoldsForTheWholeStory() {
        ChatScenario scenario = new ChatScenario("test", TWO_NPCS, "premise", List.of(
            ScenarioAct.leaf(ActNumber.of(1), "", "a"), ScenarioAct.leaf(ActNumber.of(2), "", "b")));
        ChatSession session = ChatSession.fresh(scenario);
        session.cycleActivity("marcus");

        session.advanceAct();

        assertEquals(NpcActivity.PASSIVE, session.activity().levelOf("marcus"));
    }

    @Test
    void aSceneWithEveryoneAbsentNeverReachesTheSelector() {
        ChatScenario scenario = new ChatScenario("test", TWO_NPCS, "premise", List.of(
            leafWith(ActNumber.of(1), List.of(List.of(ActivityDirective.level(NpcActivity.PASSIVE, ""))))));

        assertEquals(Set.of("elena", "marcus"), ChatSession.fresh(scenario).presentNpcIds());
        assertThrows(IllegalArgumentException.class, () -> new ChatScenario("test", TWO_NPCS, "premise", List.of(
            leafWith(ActNumber.of(1), List.of(List.of(ActivityDirective.level(NpcActivity.ABSENT, ""))))))); // tous absents
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
    void reloadScenarioDropsTheForcedLevelsOfRemovedNpcsAndKeepsTheOthers() {
        ChatSession session = twoNpcSession();
        session.cycleActivity("marcus");
        session.cycleActivity("marcus"); // marcus force absent
        session.cycleActivity("elena");  // elena force passif

        Cast freshCast = new Cast(List.of(new Npc("elena", "Elena", "sheet", ""), new Npc("clara", "Clara", "sheet", "")));
        session.reloadScenario(new ChatScenario("test", freshCast, "premise", List.of()));

        assertEquals(Set.of("elena", "clara"), session.presentNpcIds(), "marcus disparu, clara nouvelle donc presente");
        assertEquals(NpcActivity.PASSIVE, session.activity().levelOf("elena"), "le forcage d'elena est conserve");
    }

    @Test
    void anOldPresentListBecomesForcedAbsences() {
        Map<String, ForcedActivity> forced = ChatSession.forcedAbsentFrom(
            new ChatScenario("test", TWO_NPCS, "premise", List.of()), Set.of("elena"));

        assertEquals(Map.of("marcus", new ForcedActivity(NpcActivity.ABSENT, "")), forced);
    }

    @Test
    void theLanguagePickedByThePlayerWinsOverTheScenarioAndSurvivesAReload() {
        ChatScenario authored = scenario("premise", List.of()).withLanguage(storymagine.chat.coeur.domaine.scenario.ScenarioLanguage.FRENCH);
        ChatSession session = ChatSession.fresh(authored);

        session.chooseLanguage(storymagine.chat.coeur.domaine.scenario.ScenarioLanguage.ENGLISH);
        assertEquals("English", session.scenario().language().englishName());

        session.reloadScenario(authored); // the file on disk still says French
        assertEquals("English", session.scenario().language().englishName());

        session.chooseLanguage(null); // Auto : back to what the scenario declares
        assertEquals("French", session.scenario().language().englishName());
        assertEquals("French", session.authoredLanguage().englishName());
    }
}
