package storymagine.chat.coeur.domaine.agent.replyorigin;

import storymagine.chat.coeur.domaine.agent.replyorigin.ReplyOriginPromptBuilder.OriginPrompt;
import storymagine.chat.coeur.domaine.session.GenerationSettings;
import storymagine.commun.coeur.ports.GenerationOptions;
import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.ModelCallPort;

import java.util.List;

/**
 * On-demand, read-only debug look at the last reply : given the prompt a character received and the
 * reply it wrote, lists which passages of the prompt match each element of the reply and which
 * elements match none (see ReplyOriginPromptBuilder). A lead for the author, not an explanation the
 * model could truly give. Never mutates the session. See ReplyOriginAnalyst.md.
 */
public class ReplyOriginAnalyst {

    private static final String AGENT_NAME = "ReplyOriginAnalyst";

    /**
     * Lower temperature than the roleplay agents, on purpose: this maps text onto text and quotes
     * it, it does not play a character, so a steadier reading is wanted.
     */
    private static final double TEMPERATURE = 0.3;
    private static final int    TOP_K_DEFAULT = 40;
    private static final double TOP_P_DEFAULT = 0.9;
    private static final double MIN_P_DEFAULT = 0.05;
    private static final double REPEAT_PENALTY_DEFAULT = 1.05;

    private static final String TRACED_HEADER   = "TRACED:";
    private static final String UNTRACED_HEADER = "UNTRACED:";
    private static final String READING_HEADER  = "READING:";
    private static final String EMPTY_SENTINEL  = "[RIEN]";

    private final ModelCallPort llm;

    public ReplyOriginAnalyst(ModelCallPort llm) {
        this.llm = llm;
    }

    public String agentName() { return AGENT_NAME; }

    public ReplyOriginAnalystOutput call(ReplyOriginAnalystInput input) {
        OriginPrompt prompt = ReplyOriginPromptBuilder.build(input.scenario(), input.trace());

        GenerationSettings settings = input.settings() != null ? input.settings() : GenerationSettings.DEFAULT;
        GenerationOptions  options  = new GenerationOptions(List.of(), settings.maxTokens(),
            settings.topK() != null ? settings.topK() : TOP_K_DEFAULT,
            settings.topP() != null ? settings.topP() : TOP_P_DEFAULT,
            settings.minP() != null ? settings.minP() : MIN_P_DEFAULT,
            settings.repeatPenalty() != null ? settings.repeatPenalty() : REPEAT_PENALTY_DEFAULT,
            settings.contextWindow());

        String raw = llm.generate(prompt.system(), prompt.user(), TEMPERATURE,
            LlmCallContext.of(AGENT_NAME, input.scenario().name()).withThink(true), options).text();
        return parse(raw);
    }

    static ReplyOriginAnalystOutput parse(String raw) {
        StringBuilder traced   = new StringBuilder();
        StringBuilder untraced = new StringBuilder();
        StringBuilder reading  = new StringBuilder();
        StringBuilder current  = null;

        for (String line : raw.split("\n")) {
            String trimmed = line.strip();
            if (trimmed.equalsIgnoreCase(TRACED_HEADER))   { current = traced;   continue; }
            if (trimmed.equalsIgnoreCase(UNTRACED_HEADER)) { current = untraced; continue; }
            if (trimmed.equalsIgnoreCase(READING_HEADER))  { current = reading;  continue; }
            if (current == null || trimmed.isEmpty() || trimmed.equalsIgnoreCase(EMPTY_SENTINEL)) continue;
            // the lists keep one element per line ; the reading is a paragraph
            if (!current.isEmpty()) current.append(current == reading ? ' ' : '\n');
            current.append(trimmed);
        }
        return new ReplyOriginAnalystOutput(traced.toString(), untraced.toString(), reading.toString());
    }
}
