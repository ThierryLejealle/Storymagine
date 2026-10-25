package storymagine.commun.infra;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Réponse de /completion — le texte généré est dans "content", pas "message.content". */
@JsonIgnoreProperties(ignoreUnknown = true)
class LlamaServerCompletionResponse {
    public String content;
    public String error;

    @JsonProperty("tokens_predicted") public int tokensPredicted;
    @JsonProperty("tokens_evaluated") public int tokensEvaluated;
}
