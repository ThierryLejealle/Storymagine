package storymagine.chat.coeur.domaine.agent;

import org.junit.jupiter.api.Test;
import storymagine.chat.coeur.domaine.agent.chatsummarizer.ChatSummarizer;
import storymagine.chat.coeur.domaine.agent.chatsummarizer.ChatSummarizerInput;
import storymagine.chat.coeur.domaine.agent.clarityreviewer.ScenarioClarityReviewer;
import storymagine.chat.coeur.domaine.agent.clarityreviewer.ScenarioClarityReviewerInput;
import storymagine.chat.coeur.domaine.agent.continuityreviewer.ScenarioContinuityReviewer;
import storymagine.chat.coeur.domaine.agent.continuityreviewer.ScenarioContinuityReviewerInput;
import storymagine.chat.coeur.domaine.agent.nextactreadiness.NextActReadinessPromptBuilder;
import storymagine.chat.coeur.domaine.agent.npcmindstate.NpcMindStatePromptBuilder;
import storymagine.chat.coeur.domaine.scenario.ActNumber;
import storymagine.chat.coeur.domaine.scenario.Cast;
import storymagine.chat.coeur.domaine.scenario.ChatScenario;
import storymagine.chat.coeur.domaine.scenario.Npc;
import storymagine.chat.coeur.domaine.scenario.NpcOpening;
import storymagine.chat.coeur.domaine.scenario.ScenarioAct;
import storymagine.chat.coeur.domaine.scenario.ScenarioLanguage;
import storymagine.chat.coeur.domaine.session.ChatPromptBuilder;
import storymagine.chat.coeur.domaine.session.Scene;
import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.LlmResult;
import storymagine.commun.coeur.ports.ModelCallPort;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every LLM call of the chat module states the scenario's language the same way ("in French, even
 * though these instructions are in English") — and keeps its former "same language as ..." wording
 * when the scenario gives none. No real LLM : a capturing ModelCallPort and the pure prompt builders.
 */
class LanguageInPromptsTest {

    private static final String FRENCH = "in French, even though these instructions are in English";
    private static final String LEGACY = "the same language as";

    private static final Npc NPC = new Npc("elena", "", "A grumpy innkeeper.", "");

    private static ChatScenario scenario(ScenarioLanguage language) {
        return new ChatScenario("inn", new Cast(List.of(NPC)), "A stormy night.",
            List.of(ScenarioAct.leaf(ActNumber.of(1), "", "Act one."), ScenarioAct.leaf(ActNumber.of(2), "", "Act two.")),
            null, NpcOpening.NONE, language);
    }

    private static final ScenarioLanguage FR = ScenarioLanguage.parse("français");

    @Test
    void roleplayPromptFixesTheLanguageOnceAndReplacesTheOldSentence() {
        String system = ChatPromptBuilder.build(scenario(FR), new Scene(NPC, List.of(), List.of(), false), 1, "", List.of()).system();

        assertTrue(system.contains("Write every reply " + FRENCH), system);
        assertFalse(system.contains(LEGACY), "l'ancienne consigne \"meme langue\" ne doit plus figurer en doublon");
        assertTrue(count(system, "even though these instructions are in English") == 1, "la consigne de langue n'apparait qu'une fois");
    }

    @Test
    void roleplayPromptKeepsTheOldWordingWithoutALanguage() {
        String system = ChatPromptBuilder.build(scenario(ScenarioLanguage.UNSPECIFIED),
            new Scene(NPC, List.of(), List.of(), false), 1, "", List.of()).system();

        assertTrue(system.contains(LEGACY));
        assertFalse(system.contains("even though these instructions"));
        assertFalse(system.contains("{LANGUAGE}"));
    }

    @Test
    void npcMindStateAndNextActAnalysesStateTheLanguage() {
        String mind = NpcMindStatePromptBuilder.build(scenario(FR), NPC, 1, "", List.of()).system();
        String next = NextActReadinessPromptBuilder.build(scenario(FR), NPC, 1, "", List.of()).system();

        assertTrue(mind.contains("Write your answer " + FRENCH), mind);
        assertTrue(next.contains("Write your answer " + FRENCH), next);
        assertFalse(mind.contains("{LANGUAGE}") || next.contains("{LANGUAGE}"));
    }

    @Test
    void summarizerAndBothReviewersStateTheLanguage() {
        Capture summarizerLlm = new Capture("Updated summary.");
        new ChatSummarizer(summarizerLlm).call(new ChatSummarizerInput("", "Player: hi", FR));
        Capture clarityLlm = new Capture("ISSUES:\n[RIEN]\nSUGGESTIONS:\n[RIEN]");
        new ScenarioClarityReviewer(clarityLlm).call(new ScenarioClarityReviewerInput(
            "sheet", "premise", ActNumber.of(1), "t", "act", FR));
        Capture continuityLlm = new Capture("ISSUES:\n[RIEN]\nSUGGESTIONS:\n[RIEN]");
        new ScenarioContinuityReviewer(continuityLlm).call(new ScenarioContinuityReviewerInput(
            "sheet", "premise", "", ActNumber.of(1), "t", "act", FR));

        assertTrue(summarizerLlm.system.contains("Write factual prose " + FRENCH), summarizerLlm.system);
        assertTrue(clarityLlm.system.contains("Write your findings " + FRENCH), clarityLlm.system);
        assertTrue(continuityLlm.system.contains("Write your findings " + FRENCH), continuityLlm.system);
    }

    @Test
    void summarizerAndReviewersKeepTheOldWordingWithoutALanguage() {
        Capture summarizerLlm = new Capture("Updated summary.");
        new ChatSummarizer(summarizerLlm).call(new ChatSummarizerInput("", "Player: hi", ScenarioLanguage.UNSPECIFIED));

        assertTrue(summarizerLlm.system.contains("Write factual prose in " + LEGACY + " the turns"), summarizerLlm.system);
    }

    private static int count(String text, String part) {
        int n = 0;
        for (int i = text.indexOf(part); i >= 0; i = text.indexOf(part, i + part.length())) n++;
        return n;
    }

    private static class Capture implements ModelCallPort {
        private final String response;
        String system;

        Capture(String response) {
            this.response = response;
        }

        @Override
        public LlmResult generate(String systemPrompt, String userPrompt, double temperature, LlmCallContext ctx) {
            this.system = systemPrompt;
            return LlmResult.of(response);
        }

        @Override
        public int contextWindow() { return 32_768; }
    }
}
