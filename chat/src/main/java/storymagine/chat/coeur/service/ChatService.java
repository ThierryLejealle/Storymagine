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
     * Regenerates the last exchange's reply, keeping the same player line and context. Requires
     * the last turn to be an LLM reply that did NOT itself trigger an act advance — retrying past
     * an act transition isn't supported (unwinding the act state cleanly isn't worth the
     * complexity for a "just try again" button).
     */
    ChatTurnResult retry(Path chatScenariosRoot, ChatSession session);

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

    /** Max tokens the model can process in one call — for the UI's context-usage gauge. */
    int contextWindow();
}
