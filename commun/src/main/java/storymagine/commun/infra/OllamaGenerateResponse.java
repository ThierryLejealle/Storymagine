package storymagine.commun.infra;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Réponse de /api/generate — le texte généré est dans "response", pas "message.content". */
@JsonIgnoreProperties(ignoreUnknown = true)
class OllamaGenerateResponse {
    public String  response;
    public boolean done;
    public String  error;

    @JsonProperty("prompt_eval_count") public int  promptEvalCount;
    @JsonProperty("eval_count")        public int  evalCount;
    @JsonProperty("eval_duration")     public long evalDuration;
}
