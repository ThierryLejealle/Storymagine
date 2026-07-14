package storymagine.commun.infra;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
class OllamaMessage {
    public String role;
    public String content;
    /** Reasoning text Ollama returns alongside content when think:true — see OllamaAdapter.buildOllamaRequest. */
    public String thinking;

    public OllamaMessage() {}

    public OllamaMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }
}
