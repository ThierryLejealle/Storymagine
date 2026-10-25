package storymagine.commun.infra;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Réponse de /v1/chat/completions — couvre à la fois le mode sync ("message" rempli) et le
 * streaming SSE (chunks avec "delta" rempli, "message" absent). "timings" est une extension
 * propre à llama-server (absente du schéma OpenAI standard), utilisée en best-effort pour les
 * tok/s — voir LlamaServerAdapter.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class LlamaServerChatResponse {
    public List<Choice> choices;
    public Usage         usage;
    public Timings       timings;
    public String        error;

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Choice {
        public LlamaServerMessage message;
        public LlamaServerMessage delta;
        @JsonProperty("finish_reason") public String finishReason;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Usage {
        @JsonProperty("prompt_tokens")     public int promptTokens;
        @JsonProperty("completion_tokens") public int completionTokens;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Timings {
        @JsonProperty("prompt_n")     public int    promptN;
        @JsonProperty("predicted_n")  public int    predictedN;
        @JsonProperty("predicted_ms") public double predictedMs;
    }
}
