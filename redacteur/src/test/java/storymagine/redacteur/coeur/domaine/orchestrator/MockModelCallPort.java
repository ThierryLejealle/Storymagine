package storymagine.redacteur.coeur.domaine.orchestrator;

import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.LlmResult;
import storymagine.commun.coeur.ports.ModelCallPort;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mock LLM for workflow tests — returns scripted responses identified by system prompt fragments.
 * Responses are matched in insertion order; the first matching key wins.
 */
public class MockModelCallPort implements ModelCallPort {

    /** Passing CriticOutputParser response (AMELIORATION/DEFAUT format). */
    public static final String CRITIC_PASS =
        "AMELIORATION: [RIEN]\nDEFAUT_SIGNIFICATIF: [RIEN]\nDEFAUT_MAJEUR: [RIEN]";

    /** Passing ProblemScoreParser response (SCORE format). */
    public static final String SCORE_PASS = "SCORE: 10";

    /** Failing CriticOutputParser response — has a DEFAUT_SIGNIFICATIF. */
    public static final String CRITIC_FAIL =
        "AMELIORATION: [RIEN]\nDEFAUT_SIGNIFICATIF: arc narratif trop plat\nDEFAUT_MAJEUR: [RIEN]";

    /**
     * Universal passing response — safe for ALL parsers:
     *   CriticOutputParser  : AMELIORATION/DEFAUT all [RIEN] → score 10, no problems
     *   ProblemScoreParser  : SCORE: 10 → score 10, no PROBLEME: lines
     *   DeusInMachinaChecker: starts with "OK" → no leaks
     *   SequenceChecker     : no MANQUANT: lines → no failures, SCORE: 10
     *   SequenceStyleChecker: no PROBLEME: lines, SCORE: 10 → passed(7)
     */
    public static final String UNIVERSAL_PASS =
        "OK\nAMELIORATION: [RIEN]\nDEFAUT_SIGNIFICATIF: [RIEN]\nDEFAUT_MAJEUR: [RIEN]\nSCORE: 10";

    private final Map<String, String> responses;
    private final String              defaultResponse;
    private final int                 contextWindowSize;

    public MockModelCallPort(int contextWindowSize, Map<String, String> responses,
                             String defaultResponse) {
        this.contextWindowSize = contextWindowSize;
        this.responses         = new LinkedHashMap<>(responses);
        this.defaultResponse   = defaultResponse;
    }

    /** Convenience factory: one response for all calls. */
    public static MockModelCallPort uniform(int ctx, String response) {
        return new MockModelCallPort(ctx, Map.of(), response);
    }

    @Override
    public LlmResult generate(String systemPrompt, String userPrompt, double temperature, LlmCallContext ctx) {
        for (Map.Entry<String, String> entry : responses.entrySet()) {
            if (systemPrompt.contains(entry.getKey())) {
                return LlmResult.of(entry.getValue());
            }
        }
        return LlmResult.of(defaultResponse);
    }

    @Override
    public int contextWindow() {
        return contextWindowSize;
    }

    /** Builder for readable test setup. */
    public static Builder builder(int ctx) {
        return new Builder(ctx);
    }

    public static class Builder {
        private final int                 ctx;
        private final Map<String, String> map = new LinkedHashMap<>();
        private       String              fallback = "";

        Builder(int ctx) { this.ctx = ctx; }

        public Builder when(String systemFragment, String response) {
            map.put(systemFragment, response);
            return this;
        }

        public Builder otherwise(String response) {
            this.fallback = response;
            return this;
        }

        public MockModelCallPort build() {
            return new MockModelCallPort(ctx, map, fallback);
        }
    }
}
