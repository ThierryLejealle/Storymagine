package storymagine.chat.coeur.domaine.session;

/**
 * One line of the conversation history : who said it and the raw text (may contain *actions*).
 * NARRATOR turns are not said by either party — they are the author's "[...]" story-beat lines
 * (see Teaser), injected verbatim into the history when a scenario or act becomes current, so they
 * persist in the transcript and later the summary even after the fiche they came from is dropped
 * from the prompt. thinking is the model's reasoning text for an LLM turn — empty for every other
 * turn, and empty even for an LLM turn unless the session had GenerationSettings.showThinking on
 * for that call. Never persisted to disk (ChatFileStorageAdapter never writes it) — reconstructed
 * turns from history.md always get "" back, since it's ephemeral debug info for the current run.
 * npcId is which Cast member spoke this LLM turn (see Npc) — null for PLAYER/NARRATOR turns, and
 * for LLM turns predating the multi-NPC Cast (a single-Npc scenario's history read from before
 * this field existed) : ChatPromptBuilder.transcript() falls back to a generic "Character" label
 * in that case, matching the old single-character behaviour exactly.
 */
public record ChatTurn(Speaker speaker, String text, String thinking, String npcId) {

    public ChatTurn(Speaker speaker, String text) {
        this(speaker, text, "", null);
    }

    public enum Speaker { PLAYER, LLM, NARRATOR }
}
