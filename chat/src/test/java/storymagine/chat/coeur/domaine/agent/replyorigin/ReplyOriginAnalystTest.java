package storymagine.chat.coeur.domaine.agent.replyorigin;

import org.junit.jupiter.api.Test;
import storymagine.chat.coeur.domaine.scenario.Cast;
import storymagine.chat.coeur.domaine.scenario.ChatScenario;
import storymagine.chat.coeur.domaine.scenario.Npc;
import storymagine.chat.coeur.domaine.session.GenerationSettings;
import storymagine.chat.coeur.domaine.session.ReplyTrace;
import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.LlmResult;
import storymagine.commun.coeur.ports.ModelCallPort;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReplyOriginAnalystTest {

    private static ChatScenario scenario() {
        return new ChatScenario("inn", new Cast(List.of(new Npc("innkeeper", "", "A grumpy innkeeper.", ""))),
            "A stormy night at the inn.", List.of());
    }

    private static ReplyTrace trace(String thinking) {
        return new ReplyTrace("innkeeper", "SYSTEM TEXT with CURRENT SCENE: the cellar.", "Recent exchange:\nAlex: Where is the way down?",
            "*I take a lantern.* \"The cellar door is behind the barrels.\"", thinking);
    }

    private static ReplyOriginAnalystInput input(String thinking) {
        return new ReplyOriginAnalystInput(scenario(), trace(thinking), GenerationSettings.DEFAULT);
    }

    @Test
    void parsesTheThreeSectionsKeepingOneListLinePerLine() {
        CapturingModelCallPort llm = new CapturingModelCallPort("""
            TRACED:
            - the cellar door -> CURRENT SCENE : "the cellar" [direct]
            - answers the question -> the recent exchange : "Where is the way down?" [indirect]
            UNTRACED:
            - the lantern
            READING:
            The scene text weighs most.
            The lantern has no counterpart.""");

        ReplyOriginAnalystOutput out = new ReplyOriginAnalyst(llm).call(input(""));

        assertEquals("""
            - the cellar door -> CURRENT SCENE : "the cellar" [direct]
            - answers the question -> the recent exchange : "Where is the way down?" [indirect]""", out.traced());
        assertEquals("- the lantern", out.untraced());
        assertEquals("The scene text weighs most. The lantern has no counterpart.", out.reading());
    }

    @Test
    void theEmptySentinelBecomesAnEmptySection() {
        CapturingModelCallPort llm = new CapturingModelCallPort("TRACED:\n[RIEN]\nUNTRACED:\n- a detail\nREADING:\nNothing matches.");

        ReplyOriginAnalystOutput out = new ReplyOriginAnalyst(llm).call(input(""));

        assertEquals("", out.traced());
        assertEquals("- a detail", out.untraced());
    }

    @Test
    void sendsThePromptTheReplyAndTheReasoningBetweenUnmistakableSeparators() {
        CapturingModelCallPort llm = new CapturingModelCallPort("TRACED:\n[RIEN]\nUNTRACED:\n[RIEN]\nREADING:\n-");

        new ReplyOriginAnalyst(llm).call(input("The scene says cellar, so the door."));

        assertTrue(llm.lastUserPrompt.contains("===== PROMPT, SYSTEM PART =====\nSYSTEM TEXT with CURRENT SCENE: the cellar."));
        assertTrue(llm.lastUserPrompt.contains("===== PROMPT, USER PART =====\nRecent exchange:"));
        assertTrue(llm.lastUserPrompt.contains("===== REPLY =====\n*I take a lantern.*"));
        assertTrue(llm.lastUserPrompt.contains("===== REASONING BEFORE THE REPLY =====\nThe scene says cellar"));
    }

    @Test
    void withoutReasoningThatSectionIsLeftOutEntirely() {
        CapturingModelCallPort llm = new CapturingModelCallPort("TRACED:\n[RIEN]\nUNTRACED:\n[RIEN]\nREADING:\n-");

        new ReplyOriginAnalyst(llm).call(input(""));

        assertFalse(llm.lastUserPrompt.contains("REASONING"));
    }

    @Test
    void theSystemPartAsksForAPurelyDescriptiveMappingInTheScenarioLanguage() {
        CapturingModelCallPort llm = new CapturingModelCallPort("TRACED:\n[RIEN]\nUNTRACED:\n[RIEN]\nREADING:\n-");

        new ReplyOriginAnalyst(llm).call(input(""));

        assertTrue(llm.lastSystemPrompt.contains("Describe correspondences only; nothing is evaluated."));
        assertTrue(llm.lastSystemPrompt.contains("TRACED:") && llm.lastSystemPrompt.contains("UNTRACED:"));
        assertFalse(llm.lastSystemPrompt.contains("{LANGUAGE}"), "le marqueur de langue doit etre remplace");
        assertEquals(Boolean.TRUE, llm.lastCtx.think());
    }

    private static class CapturingModelCallPort implements ModelCallPort {
        private final String responseText;
        String lastSystemPrompt;
        String lastUserPrompt;
        LlmCallContext lastCtx;

        CapturingModelCallPort(String responseText) {
            this.responseText = responseText;
        }

        @Override
        public LlmResult generate(String systemPrompt, String userPrompt, double temperature, LlmCallContext ctx) {
            this.lastSystemPrompt = systemPrompt;
            this.lastUserPrompt = userPrompt;
            this.lastCtx = ctx;
            return LlmResult.of(responseText);
        }

        @Override
        public int contextWindow() { return 32_768; }
    }
}
