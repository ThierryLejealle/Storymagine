package storymagine.commun.infra;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Corps de requête pour /completion — endpoint natif llama.cpp, un seul bloc de texte sans rôles. */
class LlamaServerCompletionRequest {
    public String  prompt;
    public boolean stream = false;
    public double  temperature;

    @JsonProperty("top_k")          public int    topK;
    @JsonProperty("top_p")          public double topP;
    @JsonProperty("repeat_penalty") public double repeatPenalty;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("n_predict")
    public Integer nPredict;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public List<String> stop;
}
