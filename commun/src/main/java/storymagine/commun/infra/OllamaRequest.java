package storymagine.commun.infra;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class OllamaRequest {
    public String model;
    public List<OllamaMessage> messages;
    public boolean stream   = false;
    public boolean truncate = false;
    public Map<String, Object> options = new HashMap<>();

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Boolean think;
}
