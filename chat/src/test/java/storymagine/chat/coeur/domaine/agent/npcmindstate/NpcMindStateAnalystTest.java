package storymagine.chat.coeur.domaine.agent.npcmindstate;

import org.junit.jupiter.api.Test;
import storymagine.chat.coeur.domaine.scenario.ActNumber;
import storymagine.chat.coeur.domaine.scenario.ChatScenario;
import storymagine.chat.coeur.domaine.scenario.ScenarioAct;
import storymagine.chat.coeur.domaine.session.ChatTurn;
import storymagine.chat.coeur.domaine.session.GenerationSettings;
import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.LlmResult;
import storymagine.commun.coeur.ports.ModelCallPort;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcMindStateAnalystTest {

    private static ChatScenario scenario() {
        return new ChatScenario("inn", "A grumpy innkeeper.", "A stormy night at the inn.", List.of(
            ScenarioAct.leaf(ActNumber.of(1), "", "First act.")), "");
    }

    @Test
    void parsesSituationThoughtsAndPlansFromTheLlmResponse() {
        CapturingModelCallPort llm = new CapturingModelCallPort("""
            SITUATION:
            The player is questioning her about the missing ledger.
            THOUGHTS:
            She is nervous he might already know the truth.
            PLANS:
            Stall for time and change the subject.""");
        NpcMindStateAnalyst analyst = new NpcMindStateAnalyst(llm);

        NpcMindStateAnalystOutput output = analyst.call(new NpcMindStateAnalystInput(
            scenario(), 1, "", List.of(), GenerationSettings.DEFAULT));

        assertEquals("The player is questioning her about the missing ledger.", output.situation());
        assertEquals("She is nervous he might already know the truth.", output.thoughts());
        assertEquals("Stall for time and change the subject.", output.plans());
    }

    @Test
    void emptySentinelBecomesAnEmptyPlansSection() {
        CapturingModelCallPort llm = new CapturingModelCallPort("""
            SITUATION:
            Nothing much is happening right now.
            THOUGHTS:
            She feels at ease.
            PLANS:
            [RIEN]""");
        NpcMindStateAnalyst analyst = new NpcMindStateAnalyst(llm);

        NpcMindStateAnalystOutput output = analyst.call(new NpcMindStateAnalystInput(
            scenario(), 1, "", List.of(), GenerationSettings.DEFAULT));

        assertEquals("", output.plans());
    }

    @Test
    void sendsCharacterSheetPremiseCurrentActSummaryAndTranscript() {
        CapturingModelCallPort llm = new CapturingModelCallPort(
            "SITUATION:\nX\nTHOUGHTS:\nX\nPLANS:\n[RIEN]");
        NpcMindStateAnalyst analyst = new NpcMindStateAnalyst(llm);

        analyst.call(new NpcMindStateAnalystInput(scenario(), 1, "They arrived at dusk.",
            List.of(new ChatTurn(ChatTurn.Speaker.PLAYER, "I open the door.")), GenerationSettings.DEFAULT));

        assertTrue(llm.lastUserPrompt.contains("A grumpy innkeeper."));
        assertTrue(llm.lastUserPrompt.contains("A stormy night at the inn."));
        assertTrue(llm.lastUserPrompt.contains("First act."));
        assertTrue(llm.lastUserPrompt.contains("They arrived at dusk."));
        assertTrue(llm.lastUserPrompt.contains("I open the door."));
    }

    private static class CapturingModelCallPort implements ModelCallPort {
        private final String responseText;
        String lastUserPrompt;

        CapturingModelCallPort(String responseText) {
            this.responseText = responseText;
        }

        @Override
        public LlmResult generate(String systemPrompt, String userPrompt, double temperature, LlmCallContext ctx) {
            this.lastUserPrompt = userPrompt;
            return LlmResult.of(responseText);
        }

        @Override
        public int contextWindow() { return 32_768; }
    }
}
