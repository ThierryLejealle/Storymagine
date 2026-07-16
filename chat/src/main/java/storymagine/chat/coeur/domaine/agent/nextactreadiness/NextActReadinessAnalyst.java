package storymagine.chat.coeur.domaine.agent.nextactreadiness;

import storymagine.chat.coeur.domaine.agent.nextactreadiness.NextActReadinessPromptBuilder.NextActPrompt;
import storymagine.chat.coeur.domaine.session.GenerationSettings;
import storymagine.commun.coeur.ports.GenerationOptions;
import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.ModelCallPort;

import java.util.List;

/**
 * On-demand, read-only check of whether the current act's "[NEXT ACT]" condition has been met —
 * builds the analysis prompt (see NextActReadinessPromptBuilder) and calls the LLM. Never mutates
 * the session : no turn is appended, nothing is persisted. See NextActReadinessAnalyst.md.
 */
public class NextActReadinessAnalyst {

    private static final String AGENT_NAME = "NextActReadinessAnalyst";

    /**
     * Same defaults as RoleplayNarrator, deliberately : this asks what the character/story would
     * actually do next, not a detached "reasonable" judgment — a colder, more analytical sampling
     * setup could paper over a confusion the model would genuinely have during real play. See
     * RoleplayNarrator for the rationale behind each value.
     */
    private static final double TEMPERATURE           = 1.0;
    private static final int    TOP_K_DEFAULT          = 100;
    private static final double TOP_P_DEFAULT          = 0.98;
    private static final double MIN_P_DEFAULT          = 0.05;
    private static final double REPEAT_PENALTY_DEFAULT = 1.08;

    private static final String CONDITION_HEADER = "CONDITION:";
    private static final String STATE_HEADER     = "STATE:";
    private static final String MISSING_HEADER   = "MISSING:";
    private static final String EMPTY_SENTINEL   = "[RIEN]";

    private final ModelCallPort llm;

    public NextActReadinessAnalyst(ModelCallPort llm) {
        this.llm = llm;
    }

    public String agentName() { return AGENT_NAME; }

    public NextActReadinessAnalystOutput call(NextActReadinessAnalystInput input) {
        NextActPrompt prompt = NextActReadinessPromptBuilder.build(input.scenario(), input.speaker(), input.currentAct(),
            input.summary(), input.recentTurns());

        GenerationSettings settings = input.settings() != null ? input.settings() : GenerationSettings.DEFAULT;
        double             temperature = settings.temperature() != null ? settings.temperature() : TEMPERATURE;
        GenerationOptions  options     = new GenerationOptions(List.of(), settings.maxTokens(),
            settings.topK() != null ? settings.topK() : TOP_K_DEFAULT,
            settings.topP() != null ? settings.topP() : TOP_P_DEFAULT,
            settings.minP() != null ? settings.minP() : MIN_P_DEFAULT,
            settings.repeatPenalty() != null ? settings.repeatPenalty() : REPEAT_PENALTY_DEFAULT);

        // think=true meme si ce popup n'affiche pas la reflexion (voir RoleplayNarrator) : elle
        // ameliore la reponse elle-meme, pas seulement sa lisibilite pour le joueur.
        String raw = llm.generate(prompt.system(), prompt.user(), temperature,
            LlmCallContext.of(AGENT_NAME, input.scenario().name()).withThink(true), options).text();
        return parse(raw);
    }

    private static NextActReadinessAnalystOutput parse(String raw) {
        StringBuilder condition = new StringBuilder();
        StringBuilder state     = new StringBuilder();
        StringBuilder missing   = new StringBuilder();
        StringBuilder current   = null;

        for (String line : raw.split("\n")) {
            String trimmed = line.strip();
            if (trimmed.equalsIgnoreCase(CONDITION_HEADER)) { current = condition; continue; }
            if (trimmed.equalsIgnoreCase(STATE_HEADER))     { current = state;     continue; }
            if (trimmed.equalsIgnoreCase(MISSING_HEADER))   { current = missing;   continue; }
            if (current == null || trimmed.isEmpty() || trimmed.equalsIgnoreCase(EMPTY_SENTINEL)) continue;
            if (!current.isEmpty()) current.append(' ');
            current.append(trimmed);
        }
        return new NextActReadinessAnalystOutput(condition.toString(), state.toString(), missing.toString());
    }
}
