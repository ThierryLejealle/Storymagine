package storymagine.commun.infra;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Corps de requête pour /v1/chat/completions — format OpenAI-compatible exposé par llama-server. */
class LlamaServerChatRequest {
    public String model;
    public List<LlamaServerMessage> messages;
    public boolean stream = false;
    public double  temperature;

    @JsonProperty("top_k")          public int    topK;
    @JsonProperty("top_p")          public double topP;
    @JsonProperty("repeat_penalty") public double repeatPenalty;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("min_p")
    public Double minP;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("n_predict")
    public Integer nPredict;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public List<String> stop;
}
