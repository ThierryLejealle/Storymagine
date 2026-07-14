package storymagine.chat.coeur.ports;

import storymagine.chat.coeur.domaine.scenario.ChatScenario;
import storymagine.chat.coeur.domaine.session.ChatSession;
import storymagine.chat.coeur.domaine.session.ChatTurn;
import storymagine.chat.coeur.domaine.session.SavePoint;

import java.nio.file.Path;
import java.util.List;

/**
 * Reads/writes everything under chatScenariosRoot/{name}/ : the static scenario files
 * (character.txt, scenario.txt) and the persisted session (history.md, summary.md). One port for
 * the whole directory convention, kept to the strict minimum needed by ChatService.
 */
public interface ChatStoragePort {

    List<String> listScenarioNames(Path chatScenariosRoot);

    ChatScenario loadScenario(Path chatScenariosRoot, String name);

    /** Empty session (no turns, no summary) if nothing was persisted yet. */
    ChatSession loadSession(Path chatScenariosRoot, ChatScenario scenario);

    void saveSession(Path chatScenariosRoot, ChatSession session);

    void resetSession(Path chatScenariosRoot, ChatScenario scenario);

    /**
     * Appends turns about to be folded into the summary to a permanent, never-truncated archive —
     * history.md is rewritten short after every compaction, so the raw wording would otherwise be
     * lost for good the moment it's folded away.
     */
    void archiveFoldedTurns(Path chatScenariosRoot, ChatScenario scenario, List<ChatTurn> foldedTurns);

    /**
     * Player-triggered snapshot of session's current turns/summary/currentAct — distinct from the
     * automatic save that overwrites the same slot after every exchange (see saveSession).
     */
    SavePoint createSavePoint(Path chatScenariosRoot, ChatSession session);

    /** Newest first. */
    List<SavePoint> listSavePoints(Path chatScenariosRoot, ChatScenario scenario);

    /** The turns/summary/currentAct captured by createSavePoint — never the scenario or generationSettings. */
    ChatSession loadSavePoint(Path chatScenariosRoot, ChatScenario scenario, String saveId);

    /** No-op if saveId does not exist. */
    void deleteSavePoint(Path chatScenariosRoot, ChatScenario scenario, String saveId);
}
