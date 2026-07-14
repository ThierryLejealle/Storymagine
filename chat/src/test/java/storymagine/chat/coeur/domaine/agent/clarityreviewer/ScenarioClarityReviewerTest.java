package storymagine.chat.coeur.domaine.agent.clarityreviewer;

import org.junit.jupiter.api.Test;
import storymagine.chat.coeur.domaine.scenario.ActNumber;
import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.LlmResult;
import storymagine.commun.coeur.ports.ModelCallPort;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScenarioClarityReviewerTest {

    @Test
    void parsesIssuesAndSuggestionsFromTheLlmResponse() {
        CapturingModelCallPort llm = new CapturingModelCallPort(
            "ISSUES:\n- The [NEXT ACT] condition is vague\nSUGGESTIONS:\n- Name a concrete trigger");
        ScenarioClarityReviewer reviewer = new ScenarioClarityReviewer(llm);

        ScenarioClarityReviewerOutput output = reviewer.call(new ScenarioClarityReviewerInput(
            "A grumpy innkeeper.", "A stormy night.",
            ActNumber.of(1, 2), "The storm breaks", "Lightning strikes the roof."));

        assertEquals(java.util.List.of("The [NEXT ACT] condition is vague"), output.issues());
        assertEquals(java.util.List.of("Name a concrete trigger"), output.suggestions());
    }

    @Test
    void sendsOnlyTheCurrentActNeverAStorySoFar() {
        CapturingModelCallPort llm = new CapturingModelCallPort("ISSUES:\n[RIEN]\nSUGGESTIONS:\n[RIEN]");
        ScenarioClarityReviewer reviewer = new ScenarioClarityReviewer(llm);

        reviewer.call(new ScenarioClarityReviewerInput(
            "A grumpy innkeeper.", "A stormy night.",
            ActNumber.of(1, 2), "The storm breaks", "Lightning strikes the roof."));

        assertTrue(llm.lastUserPrompt.contains("A grumpy innkeeper."));
        assertTrue(llm.lastUserPrompt.contains("A stormy night."));
        assertTrue(llm.lastUserPrompt.contains("1.2"));
        assertTrue(llm.lastUserPrompt.contains("Lightning strikes the roof."));
        assertFalse(llm.lastUserPrompt.toLowerCase().contains("story so far"));
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
