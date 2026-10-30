package storymagine.chat.coeur.ports;

import storymagine.chat.coeur.domaine.pick.PickDraws;
import storymagine.chat.coeur.domaine.scenario.ChatScenario;
import storymagine.chat.coeur.domaine.validation.ScenarioReport;
import storymagine.chat.coeur.domaine.session.ChatSession;
import storymagine.chat.coeur.domaine.session.Compaction;
import storymagine.chat.coeur.domaine.session.SavePoint;
import storymagine.chat.coeur.domaine.session.TurnNumber;

import java.nio.file.Path;
import java.util.List;

/**
 * Reads/writes everything under chatScenariosRoot/{name}/ : the static scenario files
 * (scenario.txt, characters/) and the run generated while playing (one file per turn, one per
 * compaction, the presence state, save points, archives — see specs/chat.md, "Le run"). One port for
 * the whole directory convention, kept to the strict minimum needed by ChatService.
 */
public interface ChatStoragePort {

    List<String> listScenarioNames(Path chatScenariosRoot);

    /**
     * Throws IllegalStateException if the scenario directory still has the old layout (session files
     * next to the scenario, characters not in characters/) — the message says the old format is not supported.
     */
    ChatScenario loadScenario(Path chatScenariosRoot, String name);

    /**
     * The paragraphs drawn for this run's {pick} commands, in order of appearance in the
     * scenario ; empty for a scenario without tags, and for a run started before draws were recorded.
     */
    PickDraws pickDraws(Path chatScenariosRoot, ChatScenario scenario);

    /**
     * Static validation of a scenario as written (see ScenarioValidator) : every error listed, then the
     * warnings. Read only : no run is created, no pick draw is fixed.
     */
    ScenarioReport validateScenario(Path chatScenariosRoot, String name);

    /** The run as it stands (state just after its last turn), or a fresh session if nothing was persisted yet. */
    ChatSession loadSession(Path chatScenariosRoot, ChatScenario scenario);

    /**
     * True if the persisted run holds at least one turn spoken by the player or a character — i.e. a
     * game worth resuming. The narrator's opening lines alone (what a fresh start or a restart writes)
     * do not count : there is nothing to resume yet.
     */
    boolean hasPlayedTurns(Path chatScenariosRoot, ChatScenario scenario);

    /**
     * Read-only : the state the run was in just after turn `at` — the most recent compaction made at
     * or before that turn, then the verbatim turns that followed it up to `at`. Nothing is modified
     * on disk. IllegalArgumentException if `at` is beyond the last persisted turn.
     */
    ChatSession loadSessionAt(Path chatScenariosRoot, ChatScenario scenario, TurnNumber at);

    /**
     * Makes the persisted current run match the session : every turn of its window is written (its
     * own file, only if changed), any persisted turn or compaction numbered beyond the session's
     * last turn is deleted, and the presence/interjection/act state is rewritten.
     */
    void saveSession(Path chatScenariosRoot, ChatSession session);

    /** Persists one compaction as its own file in the run's sequence — see Compaction. */
    void writeCompaction(Path chatScenariosRoot, ChatScenario scenario, Compaction compaction);

    /** Deletes every persisted turn and compaction numbered above `after` — the destructive half of a rollback. */
    void truncateAfter(Path chatScenariosRoot, ChatScenario scenario, TurnNumber after);

    /**
     * Archives the current run (a zip of everything, never deleted outright — a long conversation is
     * too costly to lose to an accidental reset) and leaves the scenario with no current run.
     */
    void resetSession(Path chatScenariosRoot, ChatScenario scenario);

    /**
     * Player-triggered snapshot of the run as persisted — callers persist the session first (see
     * saveSession). A zip of the whole current run, history included, so rolling back still works
     * after loading it.
     */
    SavePoint createSavePoint(Path chatScenariosRoot, ChatSession session);

    /** Newest first. */
    List<SavePoint> listSavePoints(Path chatScenariosRoot, ChatScenario scenario);

    /**
     * Replaces the current run on disk with the save point's content and returns the state it holds.
     * IllegalArgumentException if saveId is malformed or unknown.
     */
    ChatSession loadSavePoint(Path chatScenariosRoot, ChatScenario scenario, String saveId);

    /** No-op if saveId does not exist. */
    void deleteSavePoint(Path chatScenariosRoot, ChatScenario scenario, String saveId);
}
