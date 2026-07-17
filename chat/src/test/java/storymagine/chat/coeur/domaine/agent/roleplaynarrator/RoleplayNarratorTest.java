package storymagine.chat.coeur.domaine.agent.roleplaynarrator;

import org.junit.jupiter.api.Test;
import storymagine.chat.coeur.domaine.scenario.Cast;
import storymagine.chat.coeur.domaine.scenario.ChatScenario;
import storymagine.chat.coeur.domaine.scenario.Npc;
import storymagine.chat.coeur.domaine.session.GenerationSettings;
import storymagine.chat.coeur.domaine.session.Scene;
import storymagine.commun.coeur.ports.GenerationOptions;
import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.LlmResult;
import storymagine.commun.coeur.ports.ModelCallPort;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RoleplayNarratorTest {

    private static RoleplayNarratorInput input(GenerationSettings settings) {
        Cast cast = new Cast(List.of(new Npc("innkeeper", "", "A grumpy innkeeper.", "")));
        ChatScenario scenario = new ChatScenario("inn", cast, "A stormy night at the inn.", List.of());
        Scene scene = new Scene(cast.npcs().get(0), List.of(), List.of(), false);
        return new RoleplayNarratorInput(scenario, scene, 0, "", List.of(), settings);
    }

    @Test
    void alwaysRequestsThinkingEvenWhenDisplayIsExplicitlyOff() {
        CapturingModelCallPort llm = new CapturingModelCallPort("*nods*");
        RoleplayNarrator narrator = new RoleplayNarrator(llm);
        GenerationSettings hidden = new GenerationSettings(null, null, null, null, Boolean.FALSE, null, null, null);

        narrator.call(input(hidden));

        assertEquals(Boolean.TRUE, llm.lastCtx.think(), "toujours demande a l'appel, meme si on ne l'affiche pas");
    }

    @Test
    void surfacesThinkingByDefaultWhenSettingsDontSayOtherwise() {
        CapturingModelCallPort llm = new CapturingModelCallPort("*nods*", "some reasoning");
        RoleplayNarrator narrator = new RoleplayNarrator(llm);

        RoleplayNarratorOutput output = narrator.call(input(GenerationSettings.DEFAULT)); // showThinking null == affiche

        assertEquals("some reasoning", output.thinking());
    }

    @Test
    void hidesThinkingWhenExplicitlyDisabled() {
        CapturingModelCallPort llm = new CapturingModelCallPort("*nods*", "some reasoning");
        RoleplayNarrator narrator = new RoleplayNarrator(llm);
        GenerationSettings hidden = new GenerationSettings(null, null, null, null, Boolean.FALSE, null, null, null);

        RoleplayNarratorOutput output = narrator.call(input(hidden));

        assertEquals("", output.thinking());
    }

    // ── Reprise sur reponse vide (evols/2026-07-15-2318) ────────────────────

    @Test
    void retriesOnceWhenTheFirstResponseComesBackBlank() {
        SequencedModelCallPort llm = new SequencedModelCallPort("", "*grunts*");
        RoleplayNarrator narrator = new RoleplayNarrator(llm);

        RoleplayNarratorOutput output = narrator.call(input(GenerationSettings.DEFAULT));

        assertEquals("*grunts*", output.replyText());
        assertEquals(2, llm.callCount);
    }

    @Test
    void doesNotRetryWhenTheFirstResponseIsAlreadyUsable() {
        SequencedModelCallPort llm = new SequencedModelCallPort("*nods*", "should never be reached");
        RoleplayNarrator narrator = new RoleplayNarrator(llm);

        narrator.call(input(GenerationSettings.DEFAULT));

        assertEquals(1, llm.callCount, "un premier texte non vide ne doit jamais declencher une 2e generation");
    }

    @Test
    void givesUpAfterOneRetryIfStillBlankRatherThanLoopingForever() {
        SequencedModelCallPort llm = new SequencedModelCallPort("", "");
        RoleplayNarrator narrator = new RoleplayNarrator(llm);

        RoleplayNarratorOutput output = narrator.call(input(GenerationSettings.DEFAULT));

        assertEquals("", output.replyText());
        assertEquals(2, llm.callCount, "une seule reprise autorisee, jamais une boucle");
    }

    /** Renvoie une reponse differente a chaque appel successif, dans l'ordre donne au constructeur. */
    private static class SequencedModelCallPort implements ModelCallPort {
        private final String[] responses;
        int callCount = 0;

        SequencedModelCallPort(String... responses) {
            this.responses = responses;
        }

        @Override
        public LlmResult generate(String systemPrompt, String userPrompt, double temperature, LlmCallContext ctx) {
            String text = responses[Math.min(callCount, responses.length - 1)];
            callCount++;
            return new LlmResult(text, 0, 0, 0, "");
        }

        @Override
        public LlmResult generate(String systemPrompt, String userPrompt, double temperature, LlmCallContext ctx,
                                   GenerationOptions options) {
            return generate(systemPrompt, userPrompt, temperature, ctx);
        }

        @Override
        public int contextWindow() { return 32_768; }
    }

    private static class CapturingModelCallPort implements ModelCallPort {
        private final String responseText;
        private final String thinkingText;
        LlmCallContext lastCtx;

        CapturingModelCallPort(String responseText) {
            this(responseText, "");
        }

        CapturingModelCallPort(String responseText, String thinkingText) {
            this.responseText = responseText;
            this.thinkingText = thinkingText;
        }

        @Override
        public LlmResult generate(String systemPrompt, String userPrompt, double temperature, LlmCallContext ctx) {
            this.lastCtx = ctx;
            return new LlmResult(responseText, 0, 0, 0, thinkingText);
        }

        @Override
        public LlmResult generate(String systemPrompt, String userPrompt, double temperature, LlmCallContext ctx,
                                   GenerationOptions options) {
            return generate(systemPrompt, userPrompt, temperature, ctx);
        }

        @Override
        public int contextWindow() { return 32_768; }
    }
}
