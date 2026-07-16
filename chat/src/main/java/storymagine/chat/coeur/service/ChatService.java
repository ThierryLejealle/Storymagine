package storymagine.chat.coeur.service;

import storymagine.chat.coeur.domaine.agent.nextactreadiness.NextActReadinessAnalystOutput;
import storymagine.chat.coeur.domaine.agent.npcmindstate.NpcMindStateAnalystOutput;
import storymagine.chat.coeur.domaine.scenario.ChatScenario;
import storymagine.chat.coeur.domaine.session.ChatSession;
import storymagine.chat.coeur.domaine.session.SavePoint;

import java.nio.file.Path;
import java.util.List;

/** Entry point of the chat core : list/load scenarios, open a session, exchange turns. */
public interface ChatService {

    List<String> listScenarios(Path chatScenariosRoot);

    ChatScenario loadScenario(Path chatScenariosRoot, String name);

    /** Loads the persisted session for this scenario, or a fresh one if reset was asked / none exists. */
    ChatSession openSession(Path chatScenariosRoot, ChatScenario scenario, boolean reset);

    ChatTurnResult sendMessage(Path chatScenariosRoot, ChatSession session, String rawPlayerInput);

    /**
     * Same as sendMessage(), but listener is notified synchronously, in order, of everything this
     * exchange produces before the whole round is done : each Npc reply's text as it grows
     * (listener.onPartialReply, token by token from the model), then each turn as it's finalized
     * and appended to session (listener.onTurnReady — the player's own turn first, then each Npc
     * reply, then any NARRATOR beat turns an act advance triggers). Lets a caller (the web UI) show
     * a reply growing in real time instead of waiting for it — or even the whole multi-Npc round —
     * to finish. The plain sendMessage() above is exactly this with ExchangeProgressListener.NOOP.
     */
    ChatTurnResult sendMessage(Path chatScenariosRoot, ChatSession session, String rawPlayerInput,
                                ExchangeProgressListener listener);

    /**
     * Regenerates the last exchange's reply, keeping the same player line and context. Requires
     * the last turn to be an LLM reply that did NOT itself trigger an act advance — retrying past
     * an act transition isn't supported (unwinding the act state cleanly isn't worth the
     * complexity for a "just try again" button).
     */
    ChatTurnResult retry(Path chatScenariosRoot, ChatSession session);

    /**
     * Like retry(), but only regenerates the current round starting from fromNpcId's reply
     * onward — any reply from another Npc earlier in the same round is kept untouched. The same
     * Npc(s) answer again, in their original primary/interjecting role : never re-selected from
     * scratch, since a fresh SpeakerSelector run could duplicate an Npc whose earlier reply is
     * being kept, or silently add/drop Npcs the player never asked to change. Regenerating from
     * an earlier reply necessarily redoes every reply after it too — a later Npc's original reply
     * was written having already seen the one being replaced, so keeping it as-is would leave it
     * reacting to text that no longer exists. Throws IllegalStateException if fromNpcId did not
     * answer in the current round.
     */
    ChatTurnResult retry(Path chatScenariosRoot, ChatSession session, String fromNpcId);

    /**
     * Drops the last `steps` player/reply exchanges (and any act-advance NARRATOR turns they
     * produced) — steps=1 is a normal single-step undo ; asking for more than the session actually
     * has clamps to everything available, back to the very first player turn. Returns how many
     * turns were removed (0 = no-op, nothing left to undo) — the UI uses the count to remove that
     * many trailing bubbles from what it already shows. Does not revert an act advance that
     * already happened — the visible turns are removed, but currentAct stays put.
     */
    int undo(Path chatScenariosRoot, ChatSession session, int steps);

    /** Manual "next act" button — advances one act. Returns false (no-op) if there is no next act. */
    boolean advanceAct(Path chatScenariosRoot, ChatSession session);

    /**
     * Manual "previous act" button — for when the story moved on too eagerly. Steps back one act
     * without touching the turn history. Returns false (no-op) if already on the first act.
     */
    boolean previousAct(Path chatScenariosRoot, ChatSession session);

    /**
     * On-demand check of whether the current act's "[NEXT ACT]" condition has actually been met —
     * read-only : no turn is appended, nothing is persisted (see NextActReadinessAnalyst.md).
     * Throws IllegalStateException if there is no next act to check (acts unused, or already on
     * the last one).
     */
    NextActReadinessAnalystOutput analyzeNextActReadiness(Path chatScenariosRoot, ChatSession session);

    /**
     * On-demand look at the character's current state of mind (situation/thoughts/plans) — read
     * only, same guarantees as analyzeNextActReadiness. Always available, no precondition.
     */
    NpcMindStateAnalystOutput analyzeMindState(Path chatScenariosRoot, ChatSession session);

    /**
     * Player-triggered snapshot of the session's current turns/summary/currentAct — distinct from
     * the automatic save that overwrites the same slot after every exchange.
     */
    SavePoint save(Path chatScenariosRoot, ChatSession session);

    /** Newest first. */
    List<SavePoint> listSavePoints(Path chatScenariosRoot, ChatScenario scenario);

    /**
     * Restores a save point into session, in place (see ChatSession.restore) — overwrites
     * session's current state outright, no automatic safety save. Callers (the chat UI) confirm
     * with the player first, since this is otherwise a silent, irreversible discard.
     */
    void loadSavePoint(Path chatScenariosRoot, ChatSession session, String saveId);

    /** No-op if saveId does not exist. */
    void deleteSavePoint(Path chatScenariosRoot, ChatScenario scenario, String saveId);

    /**
     * Mutes/unmutes one Cast member (a scene "vignette"). Returns false (no-op) if this would mute
     * the last present Npc, or if the Npc was already in the requested state — see ChatSession.
     */
    boolean setNpcPresent(Path chatScenariosRoot, ChatSession session, String npcId, boolean present);

    /**
     * Opts one Cast member in/out of unprompted interjections (see SpeakerSelector.rollInterjectors,
     * ChatSession.setInterjecting). Returns false (no-op) if the Npc was already in the requested
     * state — unlike setNpcPresent, there is no "keep at least one" guard.
     */
    boolean setNpcInterjecting(Path chatScenariosRoot, ChatSession session, String npcId, boolean interjecting);

    /**
     * Re-reads scenario.txt and every character .txt from disk into session, in place — for picking
     * up edits made to those files while the session is running, without losing the conversation so
     * far (see ChatSession.reloadScenario). Persists the reconciled presence right away.
     */
    void reloadScenario(Path chatScenariosRoot, ChatSession session);

    /**
     * Wipes the conversation back to a fresh start on the same scenario, in place (see
     * ChatSession.restore) — archives the old history/summary/act/present (see
     * ChatStoragePort.resetSession, same mechanism as the CLI's reset prompt), not a silent discard.
     * Callers (the chat UI) confirm with the player first, same guarantee as loadSavePoint.
     */
    void restartSession(Path chatScenariosRoot, ChatSession session);

    /** Max tokens the model can process in one call — for the UI's context-usage gauge. */
    int contextWindow();
}
