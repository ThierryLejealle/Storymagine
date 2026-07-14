package storymagine.chat.coeur.domaine.agent.roleplaynarrator;

import storymagine.chat.coeur.domaine.session.ChatPromptBuilder;
import storymagine.chat.coeur.domaine.session.ChatPromptBuilder.ChatPrompt;
import storymagine.chat.coeur.domaine.session.GenerationSettings;
import storymagine.commun.coeur.ports.GenerationOptions;
import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.LlmResult;
import storymagine.commun.coeur.ports.ModelCallPort;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Plays the character for one turn : builds the roleplay prompt (see ChatPromptBuilder) and calls
 * the LLM. See RoleplayNarrator.md.
 */
public class RoleplayNarrator {

    private static final String AGENT_NAME  = "RoleplayNarrator";

    /**
     * Sampling defaults tuned for creative roleplay, not Ollama's/redacteur's generic defaults —
     * this is the "Fun" preset (see chat.html), the default session experience. minP does the
     * real truncation work (keeps only tokens at least MIN_P_DEFAULT of the top token's
     * probability — scales with the model's own confidence, unlike a fixed topP/topK cutoff), so
     * topK/topP are pulled back to barely intervene rather than compounding with it.
     * repeatPenalty is the main lever against loops/mirroring in a long RP session. All five are
     * overridable per session (see GenerationSettings) except topP, never a direct dial in the
     * settings panel (redundant with minP) — only a preset changes it. Reviewed with Fable
     * (2026-07-14) alongside the "Sérieux"/"Complètement barré" presets.
     */
    private static final double TEMPERATURE       = 1.0;
    private static final int    TOP_K_DEFAULT      = 100;
    private static final double TOP_P_DEFAULT      = 0.98;
    private static final double MIN_P_DEFAULT      = 0.05;
    private static final double REPEAT_PENALTY_DEFAULT = 1.08;

    private static final Pattern NEXT_ACT_MARKER = Pattern.compile("\\s*\\[NEXT ACT\\]\\s*", Pattern.CASE_INSENSITIVE);

    /**
     * Cuts generation short the moment the model starts writing the next turn in the other
     * speaker's place — without this, a small model regularly drifts into playing the player (or
     * a narration) itself. Both prefixes already exist in the transcript sent (see
     * ChatPromptBuilder.transcript()), so the model recognizes them.
     */
    private static final List<String> STOP_SEQUENCES = List.of("\nPlayer:", "\nNarration:");

    private final ModelCallPort llm;

    public RoleplayNarrator(ModelCallPort llm) {
        this.llm = llm;
    }

    public String agentName() { return AGENT_NAME; }

    public int contextWindow() { return llm.contextWindow(); }

    public RoleplayNarratorOutput call(RoleplayNarratorInput input) {
        ChatPrompt prompt = ChatPromptBuilder.build(input.scenario(), input.currentAct(), input.summary(),
            input.recentTurns(), input.playerMessage());

        GenerationSettings settings    = input.settings() != null ? input.settings() : GenerationSettings.DEFAULT;
        double             temperature = settings.temperature() != null ? settings.temperature() : TEMPERATURE;
        GenerationOptions  options     = new GenerationOptions(STOP_SEQUENCES, settings.maxTokens(),
            settings.topK() != null ? settings.topK() : TOP_K_DEFAULT,
            settings.topP() != null ? settings.topP() : TOP_P_DEFAULT,
            settings.minP() != null ? settings.minP() : MIN_P_DEFAULT,
            settings.repeatPenalty() != null ? settings.repeatPenalty() : REPEAT_PENALTY_DEFAULT);
        boolean            showThinking = Boolean.TRUE.equals(settings.showThinking());
        LlmCallContext     ctx = LlmCallContext.of(AGENT_NAME, input.scenario().name());
        if (showThinking) ctx = ctx.withThink(true);

        LlmResult result = llm.generate(prompt.system(), prompt.user(), temperature, ctx, options);

        String rawReply = result.text().strip();
        boolean triggeredNextAct = NEXT_ACT_MARKER.matcher(rawReply).find();
        String replyText = NEXT_ACT_MARKER.matcher(rawReply).replaceAll(" ").strip();
        String thinking = showThinking ? result.thinking().strip() : "";

        return new RoleplayNarratorOutput(replyText, triggeredNextAct, thinking);
    }
}
