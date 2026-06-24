package storymagine.commun.infra;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
class OllamaResponse {
    public OllamaMessage message;
    public boolean done;
    public String error;

    @JsonProperty("prompt_eval_count") public int  promptEvalCount;
    @JsonProperty("eval_count")        public int  evalCount;
    @JsonProperty("eval_duration")     public long evalDuration;
}
