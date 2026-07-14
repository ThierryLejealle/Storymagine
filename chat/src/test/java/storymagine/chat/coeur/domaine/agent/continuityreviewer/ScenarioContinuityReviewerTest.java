package storymagine.chat.coeur.domaine.agent.continuityreviewer;

import org.junit.jupiter.api.Test;
import storymagine.chat.coeur.domaine.scenario.ActNumber;
import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.LlmResult;
import storymagine.commun.coeur.ports.ModelCallPort;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScenarioContinuityReviewerTest {

    @Test
    void parsesIssuesAndSuggestionsFromTheLlmResponse() {
        CapturingModelCallPort llm = new CapturingModelCallPort(
            "ISSUES:\n- Act 1 contradicts this act\nSUGGESTIONS:\n- Introduce the fact earlier");
        ScenarioContinuityReviewer reviewer = new ScenarioContinuityReviewer(llm);

        ScenarioContinuityReviewerOutput output = reviewer.call(new ScenarioContinuityReviewerInput(
            "A grumpy innkeeper.", "A stormy night.", "Act 1 — Arrival:\nThey arrive at the inn.\n\n",
            ActNumber.of(1, 2), "The storm breaks", "Lightning strikes the roof."));

        assertEquals(java.util.List.of("Act 1 contradicts this act"), output.issues());
        assertEquals(java.util.List.of("Introduce the fact earlier"), output.suggestions());
    }

    @Test
    void sendsCharacterSheetPremiseStorySoFarAndCurrentActInThePrompt() {
        CapturingModelCallPort llm = new CapturingModelCallPort("ISSUES:\n[RIEN]\nSUGGESTIONS:\n[RIEN]");
        ScenarioContinuityReviewer reviewer = new ScenarioContinuityReviewer(llm);

        reviewer.call(new ScenarioContinuityReviewerInput(
            "A grumpy innkeeper.", "A stormy night.", "Act 1 — Arrival:\nThey arrive at the inn.\n\n",
            ActNumber.of(1, 2), "The storm breaks", "Lightning strikes the roof."));

        assertTrue(llm.lastUserPrompt.contains("A grumpy innkeeper."));
        assertTrue(llm.lastUserPrompt.contains("A stormy night."));
        assertTrue(llm.lastUserPrompt.contains("They arrive at the inn."));
        assertTrue(llm.lastUserPrompt.contains("1.2"));
        assertTrue(llm.lastUserPrompt.contains("Lightning strikes the roof."));
    }

    @Test
    void marksTheFirstActWhenStorySoFarIsBlank() {
        CapturingModelCallPort llm = new CapturingModelCallPort("ISSUES:\n[RIEN]\nSUGGESTIONS:\n[RIEN]");
        ScenarioContinuityReviewer reviewer = new ScenarioContinuityReviewer(llm);

        reviewer.call(new ScenarioContinuityReviewerInput(
            "A grumpy innkeeper.", "A stormy night.", "",
            ActNumber.of(1), "Arrival", "They arrive at the inn."));

        assertTrue(llm.lastUserPrompt.contains("first act"));
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
