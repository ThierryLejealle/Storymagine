package storymagine.chat.coeur.domaine.session;

import storymagine.chat.coeur.domaine.scenario.ChatScenario;
import storymagine.chat.coeur.domaine.scenario.Teaser;

import java.util.ArrayList;
import java.util.List;

/**
 * The live conversation bound to one ChatScenario : accumulated turns plus the running summary
 * that older, folded-away turns get compacted into (see ChatSummarizer).
 */
public class ChatSession {

    private final ChatScenario     scenario;
    private final List<ChatTurn>   turns;
    private       String           summary;
    private       int              currentAct;
    private       GenerationSettings generationSettings = GenerationSettings.DEFAULT;

    public ChatSession(ChatScenario scenario, List<ChatTurn> turns, String summary, int currentAct) {
        this.scenario   = scenario;
        this.turns      = new ArrayList<>(turns);
        this.summary    = summary == null ? "" : summary;
        this.currentAct = currentAct;
    }

    /**
     * Fresh session — starts on act 1 if the scenario has acts, or 0 (no act system in use), and
     * opens with that act's (or, act-less, the premise's) "[...]" story-beat lines as NARRATOR
     * turns (see Teaser). Acts already carry their resolved beats (see ScenarioAct) — this leaf's
     * own plus any ancestor entered for the first time — the premise has no such distinction since
     * there is only ever one act-less state.
     */
    public static ChatSession fresh(ChatScenario scenario) {
        int startAct = scenario.acts().isEmpty() ? 0 : 1;
        List<String> openingBeats = startAct > 0
            ? scenario.acts().get(startAct - 1).beats()
            : Teaser.extractAll(scenario.premise());
        return new ChatSession(scenario, narratorTurns(openingBeats), "", startAct);
    }

    public ChatScenario scenario() { return scenario; }

    public List<ChatTurn> turns() { return List.copyOf(turns); }

    public String summary() { return summary; }

    /** 0 if the scenario has no acts, otherwise the 1-based number of the currently active act. */
    public int currentAct() { return currentAct; }

    public GenerationSettings generationSettings() { return generationSettings; }

    /** Applied to the very next RoleplayNarrator call — see GenerationSettings. */
    public void updateGenerationSettings(GenerationSettings settings) {
        this.generationSettings = settings == null ? GenerationSettings.DEFAULT : settings;
    }

    /**
     * Moves to the next act, appending the new act's "[...]" story-beat lines as NARRATOR turns
     * (see Teaser) so they persist in the history and, later, the summary even after the act's
     * full text is dropped from the prompt. Returns false (no-op) if there is no next act, or acts
     * aren't in use.
     */
    public boolean advanceAct() {
        if (currentAct <= 0 || currentAct >= scenario.acts().size()) return false;
        currentAct++;
        turns.addAll(narratorTurns(scenario.acts().get(currentAct - 1).beats()));
        return true;
    }

    /**
     * Manual correction for when the story moved on too eagerly (the LLM or the player triggered
     * the next act too soon) : steps back to the previous act. Does not touch the turn history —
     * any NARRATOR beat turns the left act added stay put, this only changes which act text is
     * sent to the LLM from now on. Returns false (no-op) if already on the first act, or acts
     * aren't in use.
     */
    public boolean previousAct() {
        if (currentAct <= 1) return false;
        currentAct--;
        return true;
    }

    private static List<ChatTurn> narratorTurns(List<String> beats) {
        return beats.stream().map(line -> new ChatTurn(ChatTurn.Speaker.NARRATOR, line)).toList();
    }

    public void append(ChatTurn turn) { turns.add(turn); }

    /** Drops every turn from index (inclusive) to the end — used to undo or retry the tail of the conversation. */
    public void truncateFrom(int index) {
        while (turns.size() > index) turns.remove(turns.size() - 1);
    }

    /** Replaces the folded-away turns with newSummary, keeping only the turns still in the window. */
    public void compactInto(String newSummary, List<ChatTurn> keptTurns) {
        this.summary = newSummary;
        this.turns.clear();
        this.turns.addAll(keptTurns);
    }

    /**
     * Replaces turns/summary/currentAct in place with a save point's content (see
     * ChatStoragePort.loadSavePoint) — the ChatSession instance itself is kept (callers, e.g.
     * ChatWebServer, hold a single long-lived reference), only its state changes. generationSettings
     * is deliberately untouched : it was never part of what a save point captures (session-only
     * tuning knob, see GenerationSettings).
     */
    public void restore(String summary, List<ChatTurn> turns, int currentAct) {
        this.summary = summary == null ? "" : summary;
        this.turns.clear();
        this.turns.addAll(turns);
        this.currentAct = currentAct;
    }
}
