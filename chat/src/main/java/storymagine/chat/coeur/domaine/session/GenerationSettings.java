package storymagine.chat.coeur.domaine.session;

/**
 * Optional per-session overrides for the roleplay LLM call (see RoleplayNarrator) — null fields
 * mean "use RoleplayNarrator's own default". Set via the UI's settings panel ; deliberately not
 * persisted to disk (ChatFileStorageAdapter never writes it) — resets to defaults on restart, a
 * tuning knob for the current session, not part of the scenario or the story itself. minP is a
 * sampling floor relative to the most likely token's probability (see RoleplayNarrator) — tends to
 * curb incoherent tail tokens while staying less restrictive than topP/topK on their own, a good
 * fit for creative roleplay generation. showThinking controls whether the model's reasoning is
 * shown to the player for this session — on by default (null and TRUE both mean "shown"; only an
 * explicit FALSE hides it), since the LLM call itself always asks for reasoning regardless of this
 * flag (see RoleplayNarrator, ChatTurn.thinking) — this only gates the display, not the request.
 * repeatPenalty discourages the model from repeating itself verbatim — the main lever against
 * loops/mirroring in a long RP session, worth exposing on its own. topP is NOT exposed as its own
 * dial in the settings panel (redundant with minP, same job of truncating the tail — confusing to
 * tune both) : it only ever changes via a preset (see chat.html), never by direct user input.
 * interjectionChance is the independent per-Npc probability (0-1) used by
 * SpeakerSelector.rollInterjectors — null falls back to RoleplayNarrator.INTERJECTION_CHANCE_DEFAULT.
 */
public record GenerationSettings(Double temperature, Integer topK, Integer maxTokens, Double minP,
                                  Boolean showThinking, Double topP, Double repeatPenalty, Double interjectionChance) {
    public static final GenerationSettings DEFAULT =
        new GenerationSettings(null, null, null, null, null, null, null, null);
}
