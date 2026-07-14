package storymagine.chat.coeur.domaine.agent.nextactreadiness;

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

class NextActReadinessAnalystTest {

    private static ChatScenario scenario() {
        return new ChatScenario("inn", "A grumpy innkeeper.", "A stormy night at the inn.", List.of(
            ScenarioAct.leaf(ActNumber.of(1), "", "First act."),
            ScenarioAct.leaf(ActNumber.of(2), "", "They explore the cellar. [NEXT ACT] once they find the hidden door.")), "");
    }

    @Test
    void parsesConditionStateAndMissingFromTheLlmResponse() {
        CapturingModelCallPort llm = new CapturingModelCallPort("""
            CONDITION:
            The story moves on once they find the hidden door in the cellar.
            STATE:
            Not met yet, they are still searching.
            MISSING:
            They still need to find the door.""");
        NextActReadinessAnalyst analyst = new NextActReadinessAnalyst(llm);

        NextActReadinessAnalystOutput output = analyst.call(new NextActReadinessAnalystInput(
            scenario(), 1, "", List.of(), GenerationSettings.DEFAULT));

        assertEquals("The story moves on once they find the hidden door in the cellar.", output.conditionUnderstood());
        assertEquals("Not met yet, they are still searching.", output.currentState());
        assertEquals("They still need to find the door.", output.missing());
    }

    @Test
    void emptySentinelBecomesAnEmptyMissingSection() {
        CapturingModelCallPort llm = new CapturingModelCallPort("""
            CONDITION:
            They must find the hidden door.
            STATE:
            Fully met, they just found it.
            MISSING:
            [RIEN]""");
        NextActReadinessAnalyst analyst = new NextActReadinessAnalyst(llm);

        NextActReadinessAnalystOutput output = analyst.call(new NextActReadinessAnalystInput(
            scenario(), 1, "", List.of(), GenerationSettings.DEFAULT));

        assertEquals("", output.missing());
    }

    @Test
    void sendsCharacterSheetPremiseCurrentActSummaryAndTranscript() {
        CapturingModelCallPort llm = new CapturingModelCallPort("CONDITION:\nX\nSTATE:\nX\nMISSING:\n[RIEN]");
        NextActReadinessAnalyst analyst = new NextActReadinessAnalyst(llm);

        analyst.call(new NextActReadinessAnalystInput(scenario(), 1, "They arrived at dusk.",
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
