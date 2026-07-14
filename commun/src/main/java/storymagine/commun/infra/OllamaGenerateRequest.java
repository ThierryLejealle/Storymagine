package storymagine.commun.infra;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.HashMap;
import java.util.Map;

/** Corps de requête pour /api/generate — mode brut, un seul champ "prompt", pas de rôles. */
class OllamaGenerateRequest {
    public String model;
    public String prompt;
    public boolean stream   = false;
    public boolean raw      = true;
    public Map<String, Object> options = new HashMap<>();

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Boolean think;
}
