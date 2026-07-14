package storymagine.chat.coeur.domaine.agent.npcmindstate;

import storymagine.chat.coeur.domaine.agent.npcmindstate.NpcMindStatePromptBuilder.MindStatePrompt;
import storymagine.chat.coeur.domaine.session.GenerationSettings;
import storymagine.commun.coeur.ports.GenerationOptions;
import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.ModelCallPort;

import java.util.List;

/**
 * On-demand, read-only look at the character's current state of mind (situation/thoughts/plans) —
 * builds the analysis prompt (see NpcMindStatePromptBuilder) and calls the LLM. Never mutates the
 * session : no turn is appended, nothing is persisted. See NpcMindStateAnalyst.md.
 */
public class NpcMindStateAnalyst {

    private static final String AGENT_NAME = "NpcMindStateAnalyst";

    /**
     * Same defaults as RoleplayNarrator, deliberately — same rationale as NextActReadinessAnalyst :
     * this asks what the character actually thinks right now, not a detached "reasonable"
     * judgment, so it must run under the same sampling conditions as real play.
     */
    private static final double TEMPERATURE           = 1.0;
    private static final int    TOP_K_DEFAULT          = 100;
    private static final double TOP_P_DEFAULT          = 0.98;
    private static final double MIN_P_DEFAULT          = 0.05;
    private static final double REPEAT_PENALTY_DEFAULT = 1.08;

    private static final String SITUATION_HEADER = "SITUATION:";
    private static final String THOUGHTS_HEADER   = "THOUGHTS:";
    private static final String PLANS_HEADER       = "PLANS:";
    private static final String EMPTY_SENTINEL     = "[RIEN]";

    private final ModelCallPort llm;

    public NpcMindStateAnalyst(ModelCallPort llm) {
        this.llm = llm;
    }

    public String agentName() { return AGENT_NAME; }

    public NpcMindStateAnalystOutput call(NpcMindStateAnalystInput input) {
        MindStatePrompt prompt = NpcMindStatePromptBuilder.build(input.scenario(), input.currentAct(),
            input.summary(), input.recentTurns());

        GenerationSettings settings = input.settings() != null ? input.settings() : GenerationSettings.DEFAULT;
        double             temperature = settings.temperature() != null ? settings.temperature() : TEMPERATURE;
        GenerationOptions  options     = new GenerationOptions(List.of(), settings.maxTokens(),
            settings.topK() != null ? settings.topK() : TOP_K_DEFAULT,
            settings.topP() != null ? settings.topP() : TOP_P_DEFAULT,
            settings.minP() != null ? settings.minP() : MIN_P_DEFAULT,
            settings.repeatPenalty() != null ? settings.repeatPenalty() : REPEAT_PENALTY_DEFAULT);

        String raw = llm.generate(prompt.system(), prompt.user(), temperature,
            LlmCallContext.of(AGENT_NAME, input.scenario().name()), options).text();
        return parse(raw);
    }

    private static NpcMindStateAnalystOutput parse(String raw) {
        StringBuilder situation = new StringBuilder();
        StringBuilder thoughts  = new StringBuilder();
        StringBuilder plans     = new StringBuilder();
        StringBuilder current   = null;

        for (String line : raw.split("\n")) {
            String trimmed = line.strip();
            if (trimmed.equalsIgnoreCase(SITUATION_HEADER)) { current = situation; continue; }
            if (trimmed.equalsIgnoreCase(THOUGHTS_HEADER))  { current = thoughts;  continue; }
            if (trimmed.equalsIgnoreCase(PLANS_HEADER))     { current = plans;     continue; }
            if (current == null || trimmed.isEmpty() || trimmed.equalsIgnoreCase(EMPTY_SENTINEL)) continue;
            if (!current.isEmpty()) current.append(' ');
            current.append(trimmed);
        }
        return new NpcMindStateAnalystOutput(situation.toString(), thoughts.toString(), plans.toString());
    }
}
