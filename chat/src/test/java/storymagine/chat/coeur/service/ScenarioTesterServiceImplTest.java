package storymagine.chat.coeur.service;

import org.junit.jupiter.api.Test;
import storymagine.chat.coeur.domaine.agent.clarityreviewer.ScenarioClarityReviewer;
import storymagine.chat.coeur.domaine.agent.continuityreviewer.ScenarioContinuityReviewer;
import storymagine.chat.coeur.domaine.scenario.ActNumber;
import storymagine.chat.coeur.domaine.scenario.ChatScenario;
import storymagine.chat.coeur.domaine.scenario.ScenarioAct;
import storymagine.chat.coeur.domaine.scenariotester.ActTestResult;
import storymagine.chat.coeur.domaine.scenariotester.ScenarioTestReport;
import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.LlmResult;
import storymagine.commun.coeur.ports.LogPort;
import storymagine.commun.coeur.ports.ModelCallPort;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScenarioTesterServiceImplTest {

    private static ChatScenario twoActScenario() {
        ScenarioAct act1 = ScenarioAct.leaf(ActNumber.of(1), "Arrival", "They arrive at the inn.");
        ScenarioAct act2 = ScenarioAct.leaf(ActNumber.of(2), "The storm", "Lightning strikes the roof.");
        return new ChatScenario("inn-test", "A grumpy innkeeper.", "A stormy night.",
            List.of(act1, act2), "Innkeeper");
    }

    @Test
    void producesOneActTestResultPerActInOrder() {
        RecordingModelCallPort continuityLlm = new RecordingModelCallPort(
            "ISSUES:\n- continuity issue\nSUGGESTIONS:\n- continuity suggestion");
        RecordingModelCallPort clarityLlm = new RecordingModelCallPort(
            "ISSUES:\n- clarity issue\nSUGGESTIONS:\n- clarity suggestion");
        ScenarioTesterServiceImpl service = new ScenarioTesterServiceImpl(
            new ScenarioContinuityReviewer(continuityLlm), new ScenarioClarityReviewer(clarityLlm), LogPort.NOOP);

        ScenarioTestReport report = service.testScenario(twoActScenario());

        assertEquals("inn-test", report.scenarioName());
        assertEquals(2, report.actResults().size());
        ActTestResult first = report.actResults().get(0);
        ActTestResult second = report.actResults().get(1);
        assertEquals(ActNumber.of(1), first.actNumber());
        assertEquals(ActNumber.of(2), second.actNumber());
        assertEquals(List.of("continuity issue"), first.continuityIssues());
        assertEquals(List.of("clarity issue"), first.clarityIssues());
        assertEquals(List.of("[Continuité] continuity suggestion", "[Clarté] clarity suggestion"), first.suggestions());
    }

    @Test
    void continuityReviewerSeesAGrowingStorySoFar() {
        RecordingModelCallPort continuityLlm = new RecordingModelCallPort("ISSUES:\n[RIEN]\nSUGGESTIONS:\n[RIEN]");
        RecordingModelCallPort clarityLlm = new RecordingModelCallPort("ISSUES:\n[RIEN]\nSUGGESTIONS:\n[RIEN]");
        ScenarioTesterServiceImpl service = new ScenarioTesterServiceImpl(
            new ScenarioContinuityReviewer(continuityLlm), new ScenarioClarityReviewer(clarityLlm), LogPort.NOOP);

        service.testScenario(twoActScenario());

        assertEquals(2, continuityLlm.userPrompts.size());
        assertTrue(continuityLlm.userPrompts.get(0).contains("first act"));
        assertTrue(continuityLlm.userPrompts.get(1).contains("They arrive at the inn."));
    }

    @Test
    void clarityReviewerNeverSeesAStorySoFar() {
        RecordingModelCallPort continuityLlm = new RecordingModelCallPort("ISSUES:\n[RIEN]\nSUGGESTIONS:\n[RIEN]");
        RecordingModelCallPort clarityLlm = new RecordingModelCallPort("ISSUES:\n[RIEN]\nSUGGESTIONS:\n[RIEN]");
        ScenarioTesterServiceImpl service = new ScenarioTesterServiceImpl(
            new ScenarioContinuityReviewer(continuityLlm), new ScenarioClarityReviewer(clarityLlm), LogPort.NOOP);

        service.testScenario(twoActScenario());

        assertEquals(2, clarityLlm.userPrompts.size());
        assertFalse(clarityLlm.userPrompts.get(1).contains("They arrive at the inn."));
    }

    private static class RecordingModelCallPort implements ModelCallPort {
        private final String responseText;
        final List<String> userPrompts = new ArrayList<>();

        RecordingModelCallPort(String responseText) {
            this.responseText = responseText;
        }

        @Override
        public LlmResult generate(String systemPrompt, String userPrompt, double temperature, LlmCallContext ctx) {
            userPrompts.add(userPrompt);
            return LlmResult.of(responseText);
        }

        @Override
        public int contextWindow() { return 32_768; }
    }
}
