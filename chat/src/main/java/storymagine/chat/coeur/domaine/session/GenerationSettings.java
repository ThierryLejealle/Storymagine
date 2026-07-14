package storymagine.chat.coeur.domaine.session;

/**
 * Optional per-session overrides for the roleplay LLM call (see RoleplayNarrator) — null fields
 * mean "use RoleplayNarrator's own default". Set via the UI's settings panel ; deliberately not
 * persisted to disk (ChatFileStorageAdapter never writes it) — resets to defaults on restart, a
 * tuning knob for the current session, not part of the scenario or the story itself. minP is a
 * sampling floor relative to the most likely token's probability (see RoleplayNarrator) — tends to
 * curb incoherent tail tokens while staying less restrictive than topP/topK on their own, a good
 * fit for creative roleplay generation. showThinking opts into Ollama's reasoning mode for this
 * session (off unless explicitly true) — extra latency/tokens per turn, only paid when a player
 * actually wants to see the model's reasoning (see RoleplayNarrator, ChatTurn.thinking).
 * repeatPenalty discourages the model from repeating itself verbatim — the main lever against
 * loops/mirroring in a long RP session, worth exposing on its own. topP is NOT exposed as its own
 * dial in the settings panel (redundant with minP, same job of truncating the tail — confusing to
 * tune both) : it only ever changes via a preset (see chat.html), never by direct user input.
 */
public record GenerationSettings(Double temperature, Integer topK, Integer maxTokens, Double minP,
                                  Boolean showThinking, Double topP, Double repeatPenalty) {
    public static final GenerationSettings DEFAULT = new GenerationSettings(null, null, null, null, null, null, null);
}
