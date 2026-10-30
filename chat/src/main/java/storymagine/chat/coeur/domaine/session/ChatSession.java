package storymagine.chat.coeur.domaine.session;

import storymagine.chat.coeur.domaine.scenario.ChatScenario;
import storymagine.chat.coeur.domaine.scenario.Npc;
import storymagine.chat.coeur.domaine.scenario.NpcOpening;
import storymagine.chat.coeur.domaine.scenario.Teaser;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The live conversation bound to one ChatScenario : the live window of turns (each carrying its
 * TurnNumber and the act in force when it was written — see NumberedTurn), the running summary
 * that older, folded-away turns get compacted into (see ChatSummarizer, Compaction), and which Cast
 * members are currently present in the scene. The window is always a run of consecutive turn
 * numbers starting at firstTurnNumber : a compaction only ever drops turns from its head.
 */
public class ChatSession {

    /** One turn of the window plus the act that was current when it was written. */
    private record Entry(ChatTurn turn, int act) {}

    private       ChatScenario     scenario;
    private final List<Entry>      entries;
    private       int              firstTurnNumber;
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

    /** Numbers turns from 1, each stamped with currentAct — see restored() to rebuild a window from persisted numbers. */
    public ChatSession(ChatScenario scenario, List<ChatTurn> turns, String summary, int currentAct,
                        Set<String> presentNpcIds, Set<String> interjectingNpcIds) {
        this(scenario, 1, entriesOf(turns, currentAct), summary, currentAct, presentNpcIds, interjectingNpcIds);
    }

    private ChatSession(ChatScenario scenario, int firstTurnNumber, List<Entry> entries, String summary,
                         int currentAct, Set<String> presentNpcIds, Set<String> interjectingNpcIds) {
        this.scenario           = scenario;
        this.firstTurnNumber    = firstTurnNumber;
        this.entries            = new ArrayList<>(entries);
        this.summary            = summary == null ? "" : summary;
        this.currentAct         = currentAct;
        this.presentNpcIds      = new LinkedHashSet<>(presentNpcIds);
        this.interjectingNpcIds = new LinkedHashSet<>(interjectingNpcIds);
    }

    /**
     * A session rebuilt from persisted turns : turns must be consecutive and start exactly at
     * firstTurnNumber (an empty window is allowed — a compaction that folded everything), otherwise
     * the persisted files are inconsistent and loading fails loudly instead of silently renumbering.
     */
    public static ChatSession restored(ChatScenario scenario, String summary, TurnNumber firstTurnNumber,
                                        List<NumberedTurn> turns, int currentAct, Set<String> presentNpcIds,
                                        Set<String> interjectingNpcIds) {
        List<Entry> entries = new ArrayList<>();
        int expected = firstTurnNumber.value();
        for (NumberedTurn numbered : turns) {
            if (numbered.number().value() != expected) {
                throw new IllegalStateException("Tours non consecutifs : attendu #" + expected
                    + ", trouve #" + numbered.number().value());
            }
            entries.add(new Entry(numbered.turn(), numbered.act()));
            expected++;
        }
        return new ChatSession(scenario, firstTurnNumber.value(), entries, summary, currentAct,
            presentNpcIds, interjectingNpcIds);
    }

    private static List<Entry> entriesOf(List<ChatTurn> turns, int act) {
        return turns.stream().map(turn -> new Entry(turn, act)).toList();
    }

    /** Every id in scenario.cast() — the default presence/interjection-eligibility for a fresh session or a run with no state file. */
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
        // Un scenario sans actes n'a qu'UN SEUL etat d'ouverture, pas une pile a deverser d'un
        // coup : meme convention que scenario.acts() (toujours un seul beat d'ouverture par acte).
        // Sans ce plafond, une premisse plate contenant plusieurs blocs "[...]" (plusieurs scenes
        // narratives distinctes, pas forcement pensees comme des "actes") les affichait tous a la
        // fois avant meme le premier message du joueur.
        List<String> allBeats = startAct > 0
            ? scenario.acts().get(startAct - 1).beats()
            : Teaser.extractAll(scenario.premise());
        List<String> openingBeats = startAct > 0 || allBeats.isEmpty() ? allBeats : List.of(allBeats.get(0));
        return new ChatSession(scenario, narratorTurns(openingBeats), "", startAct);
    }

    public ChatScenario scenario() { return scenario; }

    public List<ChatTurn> turns() {
        return entries.stream().map(Entry::turn).toList();
    }

    /** The live window with each turn's number and act — what gets persisted, one file per turn. */
    public List<NumberedTurn> numberedTurns() {
        List<NumberedTurn> numbered = new ArrayList<>(entries.size());
        for (int i = 0; i < entries.size(); i++) {
            numbered.add(new NumberedTurn(TurnNumber.of(firstTurnNumber + i), entries.get(i).act(), entries.get(i).turn()));
        }
        return numbered;
    }

    /** Number of the first turn still in the live window (the turns before it are folded into the summary). */
    public TurnNumber firstTurnNumber() { return TurnNumber.of(firstTurnNumber); }

    /** Number of the last turn of the run so far — one below firstTurnNumber when the window is empty. */
    public TurnNumber lastTurnNumber() { return TurnNumber.of(firstTurnNumber + entries.size() - 1); }

    public String summary() { return summary; }

    /** 0 if the scenario has no acts, otherwise the 1-based number of the currently active act. */
    private ReplyTrace lastReplyTrace;

    /** Remembers the prompt behind the reply just appended (debug, never persisted, see ReplyOriginAnalyst). */
    public void rememberReplyTrace(ReplyTrace trace) { this.lastReplyTrace = trace; }

    /**
     * The trace of the last Npc reply, only while that reply is still the last LLM turn of the
     * conversation : after an undo, a rollback, a restart or a load, what was remembered no longer
     * matches and nothing is offered.
     */
    public Optional<ReplyTrace> lastReplyTrace() {
        if (lastReplyTrace == null) return Optional.empty();
        for (int i = entries.size() - 1; i >= 0; i--) {
            ChatTurn turn = entries.get(i).turn();
            if (turn.speaker() != ChatTurn.Speaker.LLM) continue;
            boolean same = lastReplyTrace.reply().equals(turn.text())
                && java.util.Objects.equals(lastReplyTrace.npcId(), turn.npcId());
            return same ? Optional.of(lastReplyTrace) : Optional.empty();
        }
        return Optional.empty();
    }

    public int currentAct() { return currentAct; }

    /**
     * Who should speak first right now, if the story or the current act has just begun and its
     * author asked for an Npc to open (see NpcOpening) : NONE as soon as anyone but the narrator has
     * spoken in the current act. Derived from the turns, never stored — nothing to persist or keep in
     * sync across rollback, save points and restarts.
     */
    public NpcOpening pendingNpcOpening() { return npcOpeningAt(entries.size()); }

    /**
     * Same question for an empty "DO:" that sits (or would sit) at window position `index` — lets a
     * retry of that very turn find the Npc the author asked for, after the turn is already there.
     * The act is the one that turn was written in (the current act for a turn not yet appended) ;
     * only the turns before it count. After a compaction the story/act start is no longer in the
     * window : an act-less or first-act story with a summary, or an empty window, never reopens.
     */
    public NpcOpening npcOpeningAt(int index) {
        int act = index < entries.size() ? entries.get(index).act() : currentAct;
        NpcOpening opening = scenario.openingOfAct(act);
        if (opening.isNone()) return NpcOpening.NONE;
        if (act <= 1 && !summary.isBlank()) return NpcOpening.NONE;
        if (index == 0 && firstTurnNumber > 1) return NpcOpening.NONE;
        for (int i = 0; i < index; i++) {
            Entry e = entries.get(i);
            if (e.act() == act && e.turn().speaker() != ChatTurn.Speaker.NARRATOR) return NpcOpening.NONE;
        }
        return opening;
    }

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
     * full text is dropped from the prompt. Those beats are stamped with the NEW act. Returns false
     * (no-op) if there is no next act, or acts aren't in use.
     */
    public boolean advanceAct() {
        if (currentAct <= 0 || currentAct >= scenario.acts().size()) return false;
        currentAct++;
        for (ChatTurn beat : narratorTurns(scenario.acts().get(currentAct - 1).beats())) append(beat);
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

    /** Appends turn with the next TurnNumber, stamped with the act in force right now. */
    public void append(ChatTurn turn) { entries.add(new Entry(turn, currentAct)); }

    /** Drops every turn from index (inclusive) to the end — used to undo or retry the tail of the conversation. */
    public void truncateFrom(int index) {
        while (entries.size() > index) entries.remove(entries.size() - 1);
    }

    /**
     * Replaces the folded-away turns with newSummary, keeping only the turns still in the window.
     * keptTurns must be the tail of the current window. Returns the Compaction this just made, for
     * the caller to persist : createdAfter is the last turn right now, foldedUpTo the one just
     * before the first kept turn.
     */
    public Compaction compactInto(String newSummary, List<ChatTurn> keptTurns) {
        int dropped = entries.size() - keptTurns.size();
        if (dropped < 0) throw new IllegalArgumentException("Plus de tours a garder que dans la fenetre.");
        TurnNumber createdAfter = lastTurnNumber();
        TurnNumber foldedUpTo   = TurnNumber.of(firstTurnNumber + dropped - 1);
        entries.subList(0, dropped).clear();
        firstTurnNumber += dropped;
        this.summary = newSummary == null ? "" : newSummary;
        return new Compaction(createdAfter, foldedUpTo, this.summary);
    }

    /**
     * Replaces turns/summary/currentAct/presentNpcIds in place with fresh content (a fresh or
     * restarted run), numbering the turns from 1 — the ChatSession instance itself is kept
     * (callers, e.g. ChatWebServer, hold a single long-lived reference), only its state changes.
     * generationSettings is deliberately untouched : session-only tuning knob, see GenerationSettings.
     */
    public void restore(String summary, List<ChatTurn> turns, int currentAct, Set<String> presentNpcIds,
                         Set<String> interjectingNpcIds) {
        this.summary = summary == null ? "" : summary;
        this.entries.clear();
        this.entries.addAll(entriesOf(turns, currentAct));
        this.firstTurnNumber = 1;
        this.currentAct = currentAct;
        this.presentNpcIds.clear();
        this.presentNpcIds.addAll(presentNpcIds);
        this.interjectingNpcIds.clear();
        this.interjectingNpcIds.addAll(interjectingNpcIds);
    }

    /**
     * Replaces the whole run state in place with other's — window with its numbers, summary, act and
     * presence — for loading a save point. generationSettings stays untouched, as for restore().
     */
    public void restoreFrom(ChatSession other) {
        restoreConversationFrom(other);
        this.presentNpcIds.clear();
        this.presentNpcIds.addAll(other.presentNpcIds);
        this.interjectingNpcIds.clear();
        this.interjectingNpcIds.addAll(other.interjectingNpcIds);
    }

    /**
     * Replaces only the conversation (window with its numbers, summary, current act) with other's,
     * leaving presence/interjection untouched — a rollback rewinds what was said, never the
     * settings the player changed himself. A turn already in memory under the same number and
     * content is kept as-is rather than replaced by its reloaded twin, so its (never persisted)
     * thinking text survives.
     */
    public void restoreConversationFrom(ChatSession other) {
        List<Entry> next = new ArrayList<>();
        for (NumberedTurn numbered : other.numberedTurns()) {
            ChatTurn kept = numbered.turn();
            int idx = numbered.number().value() - this.firstTurnNumber;
            if (idx >= 0 && idx < this.entries.size() && sameContent(this.entries.get(idx).turn(), kept)) {
                kept = this.entries.get(idx).turn();
            }
            next.add(new Entry(kept, numbered.act()));
        }
        this.entries.clear();
        this.entries.addAll(next);
        this.firstTurnNumber = other.firstTurnNumber;
        this.summary = other.summary;
        this.currentAct = other.currentAct;
    }

    private static boolean sameContent(ChatTurn a, ChatTurn b) {
        return a.speaker() == b.speaker()
            && java.util.Objects.equals(a.npcId(), b.npcId())
            && a.text().strip().equals(b.text().strip());
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
