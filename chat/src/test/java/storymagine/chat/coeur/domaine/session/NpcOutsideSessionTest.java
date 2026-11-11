package storymagine.chat.coeur.domaine.session;

import org.junit.jupiter.api.Test;
import storymagine.chat.coeur.domaine.activity.ActivityDirective;
import storymagine.chat.coeur.domaine.activity.NpcActivity;
import storymagine.chat.coeur.domaine.scenario.ActNumber;
import storymagine.chat.coeur.domaine.scenario.Cast;
import storymagine.chat.coeur.domaine.scenario.CharacterDelta;
import storymagine.chat.coeur.domaine.scenario.ChatScenario;
import storymagine.chat.coeur.domaine.scenario.Npc;
import storymagine.chat.coeur.domaine.scenario.NpcOpening;
import storymagine.chat.coeur.domaine.scenario.ScenarioAct;
import storymagine.chat.coeur.domaine.scenario.ScenarioLanguage;
import storymagine.chat.coeur.domaine.scenario.ScenarioStyle;
import storymagine.chat.coeur.domaine.scenario.StartingRoster;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A character outside the story is out of every session view : not present, not clickable, never brought back by a forced level. */
class NpcOutsideSessionTest {

    private static ChatSession session(int act) {
        Cast cast = new Cast(List.of(new Npc("elena", "Elena", "sheet", ""), new Npc("orlo", "Orlo", "sheet", "")));
        ScenarioAct first = new ScenarioAct(ActNumber.of(1), "One", "text", List.of(), NpcOpening.NONE, List.<CharacterDelta>of(), List.of(List.of()));
        ScenarioAct second = new ScenarioAct(ActNumber.of(2), "Two", "text", List.of(), NpcOpening.NONE, List.<CharacterDelta>of(),
            List.of(List.of(ActivityDirective.arrives("Orlo"))));
        ChatScenario scenario = new ChatScenario("test", cast, "Premise.", List.of(first, second), null, NpcOpening.NONE,
            ScenarioLanguage.UNSPECIFIED, ScenarioStyle.NONE, new StartingRoster("Elena"));
        return new ChatSession(scenario, List.of(), "", act);
    }

    @Test
    void anOutsiderIsNeitherPresentNorListedAsAbsentTeammate() {
        ChatSession session = session(1);

        assertEquals(Set.of("elena"), session.presentNpcIds());
        assertEquals(Set.of("orlo"), session.outsideNpcIds());
    }

    @Test
    void thePlayersClickCannotReachSomeoneOutsideTheStory() {
        ChatSession session = session(1);

        assertFalse(session.cycleActivity("orlo"));
        assertTrue(session.forcedActivities().isEmpty());
    }

    @Test
    void aForcedLevelNeverBringsAnOutsiderBack() {
        ChatSession session = session(2);
        assertTrue(session.cycleActivity("orlo")); // orlo has arrived : clickable
        assertEquals(NpcActivity.PASSIVE, session.activity().levelOf("orlo"));

        session.previousAct(); // back to act 1 : orlo is outside again, the forced level must not resurrect him

        assertTrue(session.activity().levelOf("orlo").isOutside());
        assertEquals(Set.of("elena"), session.presentNpcIds());
    }

    @Test
    void theArrivalComesWithTheAct() {
        ChatSession session = session(1);

        session.advanceAct();

        assertEquals(Set.of("elena", "orlo"), session.presentNpcIds());
        assertTrue(session.outsideNpcIds().isEmpty());
    }
}
