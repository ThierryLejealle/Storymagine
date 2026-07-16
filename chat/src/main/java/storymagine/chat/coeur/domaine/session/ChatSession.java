package storymagine.chat.coeur.domaine.session;

import storymagine.chat.coeur.domaine.scenario.ChatScenario;
import storymagine.chat.coeur.domaine.scenario.Npc;
import storymagine.chat.coeur.domaine.scenario.Teaser;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The live conversation bound to one ChatScenario : accumulated turns, the running summary that
 * older, folded-away turns get compacted into (see ChatSummarizer), and which Cast members are
 * currently present in the scene.
 */
public class ChatSession {

    private       ChatScenario     scenario;
    private final List<ChatTurn>   turns;
    private       String           summary;
    private       int              currentAct;
    private final Set<String>      presentNpcIds;
    private final Set<String>      interjectingNpcIds;
    private       GenerationSettings generationSettings = GenerationSettings.DEFAULT;

    /** Every Npc in the scenario starts present and interjection-eligible — see setPresent/setInterjecting to opt out. */
    public ChatSession(ChatScenario scenario, List<ChatTurn> turns, String summary, int currentAct) {
        this(scenario, turns, summary, currentAct, allNpcIds(scenario));
    }

    public ChatSession(ChatScenario scenario, List<ChatTurn> turns, String summary, int currentAct,
                        Set<String> presentNpcIds) {
        this(scenario, turns, summary, currentAct, presentNpcIds, allNpcIds(scenario));
    }

    public ChatSession(ChatScenario scenario, List<ChatTurn> turns, String summary, int currentAct,
                        Set<String> presentNpcIds, Set<String> interjectingNpcIds) {
        this.scenario           = scenario;
        this.turns              = new ArrayList<>(turns);
        this.summary            = summary == null ? "" : summary;
        this.currentAct         = currentAct;
        this.presentNpcIds      = new LinkedHashSet<>(presentNpcIds);
        this.interjectingNpcIds = new LinkedHashSet<>(interjectingNpcIds);
    }

    /** Every id in scenario.cast() — the default presence/interjection-eligibility for a fresh session or a legacy save with no present.txt/interject.txt. */
    public static Set<String> allPresent(ChatScenario scenario) {
        return allNpcIds(scenario);
    }

    private static Set<String> allNpcIds(ChatScenario scenario) {
        return scenario.cast().npcs().stream().map(Npc::id).collect(Collectors.toCollection(LinkedHashSet::new));
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

    public Set<String> presentNpcIds() { return Set.copyOf(presentNpcIds); }

    /**
     * Mutes/unmutes one Cast member. Muting the last present Npc is a no-op (returns false) —
     * someone always has to be present to answer ; unmuting an unknown id is accepted as-is (the
     * session doesn't police it against scenario.cast(), callers are expected to).
     */
    public boolean setPresent(String npcId, boolean present) {
        if (present) return presentNpcIds.add(npcId);
        if (presentNpcIds.size() <= 1) return false;
        return presentNpcIds.remove(npcId);
    }

    public Set<String> interjectingNpcIds() { return Set.copyOf(interjectingNpcIds); }

    /**
     * Opts one Cast member in/out of unprompted interjections (see SpeakerSelector.rollInterjectors)
     * — unlike setPresent, no "keep at least one" guard : interjection is an optional flourish, not
     * something the session needs at least one of to keep working.
     */
    public boolean setInterjecting(String npcId, boolean interjecting) {
        return interjecting ? interjectingNpcIds.add(npcId) : interjectingNpcIds.remove(npcId);
    }

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
     * Replaces turns/summary/currentAct/presentNpcIds in place with a save point's content (see
     * ChatStoragePort.loadSavePoint) — the ChatSession instance itself is kept (callers, e.g.
     * ChatWebServer, hold a single long-lived reference), only its state changes. generationSettings
     * is deliberately untouched : it was never part of what a save point captures (session-only
     * tuning knob, see GenerationSettings).
     */
    public void restore(String summary, List<ChatTurn> turns, int currentAct, Set<String> presentNpcIds,
                         Set<String> interjectingNpcIds) {
        this.summary = summary == null ? "" : summary;
        this.turns.clear();
        this.turns.addAll(turns);
        this.currentAct = currentAct;
        this.presentNpcIds.clear();
        this.presentNpcIds.addAll(presentNpcIds);
        this.interjectingNpcIds.clear();
        this.interjectingNpcIds.addAll(interjectingNpcIds);
    }

    /**
     * Re-reads the scenario/cast from disk into this session, without touching turns/summary/
     * currentAct — for editing scenario.txt or a character's .txt file while a session is running
     * and picking up the change without losing the conversation so far. Presence is reconciled
     * against the new cast : Npc ids removed from the cast drop out of presentNpcIds, ids newly
     * added by the edit start present (same default as a fresh session), ids already present or
     * muted keep their state. Falls back to every new Npc present if that reconciliation would
     * otherwise leave nobody present (e.g. the whole present cast got renamed).
     */
    public void reloadScenario(ChatScenario freshScenario) {
        Set<String> freshIds = allNpcIds(freshScenario);
        Set<String> oldIds = allNpcIds(scenario);
        // Reconcilie AVANT de vider les champs : reconcile() lit presentNpcIds/interjectingNpcIds
        // tels qu'ils etaient avant le reload, sinon on reconcilierait depuis un set deja vide.
        Set<String> reconciledPresent = reconcile(presentNpcIds, oldIds, freshIds);
        Set<String> reconciledInterjecting = reconcile(interjectingNpcIds, oldIds, freshIds);
        this.presentNpcIds.clear();
        this.presentNpcIds.addAll(reconciledPresent);
        this.interjectingNpcIds.clear();
        this.interjectingNpcIds.addAll(reconciledInterjecting);
        this.scenario = freshScenario;
    }

    /**
     * ids still in freshIds, minus those no longer in the cast, plus any id in freshIds that
     * wasn't in oldIds yet (a newly added Npc starts opted in) — falls back to every fresh id if
     * that leaves nothing (e.g. the whole opted-in cast got renamed).
     */
    private static Set<String> reconcile(Set<String> current, Set<String> oldIds, Set<String> freshIds) {
        Set<String> reconciled = new LinkedHashSet<>(current);
        reconciled.retainAll(freshIds);
        for (String id : freshIds) {
            if (!oldIds.contains(id)) reconciled.add(id);
        }
        return reconciled.isEmpty() ? freshIds : reconciled;
    }
}
