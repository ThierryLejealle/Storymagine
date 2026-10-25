package storymagine.commun.infra;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
class LlamaServerMessage {
    public String role;
    public String content;

    /** Réflexion renvoyée par llama-server quand le serveur est lancé avec --reasoning-format
     * et un template compatible — voir LlamaServerAdapter. Jamais demandé côté requête (pas de
     * champ équivalent au think:true d'Ollama), lu tel quel s'il est présent dans la réponse.
     * NON_NULL est indispensable ici : ce POJO sert aussi aux messages de requête (system/user),
     * où ce champ vaut toujours null — sans l'annotation, Jackson le sérialise en
     * "reasoning_content":null, que llama-server rejette (HTTP 500, "type must be string,
     * but is null"), reproduit et corrigé le 2026-09-14. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("reasoning_content")
    public String reasoningContent;

    public LlamaServerMessage() {}

    public LlamaServerMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }
}
