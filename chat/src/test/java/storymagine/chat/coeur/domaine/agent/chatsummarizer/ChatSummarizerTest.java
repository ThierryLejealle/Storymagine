package storymagine.chat.coeur.domaine.agent.chatsummarizer;

import org.junit.jupiter.api.Test;
import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.LlmResult;
import storymagine.commun.coeur.ports.ModelCallPort;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatSummarizerTest {

    @Test
    void alwaysRequestsThinkingEvenThoughItIsNeverDisplayed() {
        CapturingModelCallPort llm = new CapturingModelCallPort("Updated summary.");
        ChatSummarizer summarizer = new ChatSummarizer(llm);

        summarizer.call(new ChatSummarizerInput("Previous summary.", "Player: hello\nInnkeeper: welcome"));

        assertEquals(Boolean.TRUE, llm.lastCtx.think());
    }

    private static class CapturingModelCallPort implements ModelCallPort {
        private final String responseText;
        LlmCallContext lastCtx;

        CapturingModelCallPort(String responseText) {
            this.responseText = responseText;
        }

        @Override
        public LlmResult generate(String systemPrompt, String userPrompt, double temperature, LlmCallContext ctx) {
            this.lastCtx = ctx;
            return LlmResult.of(responseText);
        }

        @Override
        public int contextWindow() { return 32_768; }
    }
}
