package storymagine.chat.coeur.domaine.agent.roleplaynarrator;

import storymagine.chat.coeur.domaine.scenario.Npc;
import storymagine.chat.coeur.domaine.session.ChatPromptBuilder;
import storymagine.chat.coeur.domaine.session.ChatPromptBuilder.ChatPrompt;
import storymagine.chat.coeur.domaine.session.GenerationSettings;
import storymagine.chat.coeur.domaine.session.Scene;
import storymagine.commun.coeur.ports.GenerationOptions;
import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.LlmResult;
import storymagine.commun.coeur.ports.ModelCallPort;

import java.util.ArrayList;
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

    /**
     * Default per-Npc probability used by SpeakerSelector.rollInterjectors when
     * GenerationSettings.interjectionChance is null — public : ChatServiceImpl (a different
     * package, orchestrates the roll before calling this agent) needs the same default, and a
     * single constant here keeps the settings-panel placeholder and the actual fallback in sync.
     */
    public static final double INTERJECTION_CHANCE_DEFAULT = 0.5;

    private static final Pattern NEXT_ACT_MARKER = Pattern.compile("\\s*\\[NEXT ACT\\]\\s*", Pattern.CASE_INSENSITIVE);

    /**
     * Cuts generation short the moment the model starts writing the next turn in someone else's
     * place — without this, a small model regularly drifts into playing the player (or a
     * narration) itself. "\n{playerName}:"/"\nNarration:" plus one stop per other present Npc's
     * label (see stopSequencesFor) — all of these prefixes already exist in the transcript sent
     * (see ChatPromptBuilder.transcript()), so the model recognizes them.
     */
    private static final String NARRATION_STOP_SEQUENCE = "\nNarration:";

    private final ModelCallPort llm;

    public RoleplayNarrator(ModelCallPort llm) {
        this.llm = llm;
    }

    public String agentName() { return AGENT_NAME; }

    public int contextWindow() { return llm.contextWindow(); }

    public RoleplayNarratorOutput call(RoleplayNarratorInput input) {
        ChatPrompt prompt = ChatPromptBuilder.build(input.scenario(), input.scene(), input.currentAct(),
            input.summary(), input.recentTurns());

        GenerationSettings settings    = input.settings() != null ? input.settings() : GenerationSettings.DEFAULT;
        double             temperature = settings.temperature() != null ? settings.temperature() : TEMPERATURE;
        GenerationOptions  options     = new GenerationOptions(
            stopSequencesFor(input.scenario().playerName(), input.scene()), settings.maxTokens(),
            settings.topK() != null ? settings.topK() : TOP_K_DEFAULT,
            settings.topP() != null ? settings.topP() : TOP_P_DEFAULT,
            settings.minP() != null ? settings.minP() : MIN_P_DEFAULT,
            settings.repeatPenalty() != null ? settings.repeatPenalty() : REPEAT_PENALTY_DEFAULT);
        // Affichage actif par defaut (null = "montre") — seul un FALSE explicite le desactive,
        // voir GenerationSettings.showThinking.
        boolean            showThinking = !Boolean.FALSE.equals(settings.showThinking());
        // Toujours demande, meme si showThinking est faux : la reflexion ameliore la reponse elle-meme
        // (voir evols/2026-07-10-2307), pas seulement son affichage — showThinking ne controle que si
        // on la montre au joueur (ci-dessous), pas si on la demande au LLM (a charge de l'adaptateur
        // de s'en debrouiller, voir ModelCallPort).
        LlmCallContext     ctx = LlmCallContext.of(AGENT_NAME, input.scenario().name()).withThink(true);

        LlmResult result = llm.generate(prompt.system(), prompt.user(), temperature, ctx, options);
        if (result.text().isBlank()) {
            // Observed in practice (evols/2026-07-15-2318) : a small model can burn its whole
            // token budget reasoning in circles and return nothing at all. One retry, same prompt
            // — sampling is stochastic (no seed pinned), so a second attempt usually just works.
            // Give up after that : a loop here would risk masking a real, persistent problem.
            result = llm.generate(prompt.system(), prompt.user(), temperature, ctx, options);
        }

        String rawReply = result.text().strip();
        boolean triggeredNextAct = NEXT_ACT_MARKER.matcher(rawReply).find();
        String replyText = NEXT_ACT_MARKER.matcher(rawReply).replaceAll(" ").strip();
        String thinking = showThinking ? result.thinking().strip() : "";

        return new RoleplayNarratorOutput(replyText, triggeredNextAct, thinking);
    }

    private static List<String> stopSequencesFor(String playerName, Scene scene) {
        List<String> stops = new ArrayList<>(List.of("\n" + playerName + ":", NARRATION_STOP_SEQUENCE));
        for (Npc other : scene.otherPresent()) {
            stops.add("\n" + other.label() + ":");
        }
        return stops;
    }
}
