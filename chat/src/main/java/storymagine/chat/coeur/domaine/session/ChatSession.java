package storymagine.chat.coeur.domaine.session;

import storymagine.chat.coeur.domaine.activity.ActivityResolver;
import storymagine.chat.coeur.domaine.activity.ActivitySource;
import storymagine.chat.coeur.domaine.activity.CastActivity;
import storymagine.chat.coeur.domaine.activity.ForcedActivity;
import storymagine.chat.coeur.domaine.activity.NpcActivity;
import storymagine.chat.coeur.domaine.activity.NpcActivityState;
import storymagine.chat.coeur.domaine.scenario.ChatScenario;
import storymagine.chat.coeur.domaine.scenario.Npc;
import storymagine.chat.coeur.domaine.activity.NpcMode;
import storymagine.chat.coeur.domaine.scenario.NpcOpening;
import storymagine.chat.coeur.domaine.scenario.ScenarioLanguage;
import storymagine.chat.coeur.domaine.scenario.Teaser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
    /** The language the scenario file declares ; scenario carries the one in force (see chooseLanguage). */
    private       ScenarioLanguage authoredLanguage;
    /** The language the player picked for this game, null = the scenario's own ("Auto"). */
    private       ScenarioLanguage chosenLanguage;
    private final List<Entry>      entries;
    private       int              firstTurnNumber;
    private       String           summary;
    private       int              currentAct;
    /** Levels the player forced by clicking, by npc id — see activity(), cycleActivity(). */
    private final Map<String, ForcedActivity> forced = new LinkedHashMap<>();
    private       GenerationSettings generationSettings = GenerationSettings.DEFAULT;

    /** Every Npc starts with the activity the scenario gives it (present, implicit unless a command says otherwise). */
    public ChatSession(ChatScenario scenario, List<ChatTurn> turns, String summary, int currentAct) {
        this(scenario, turns, summary, currentAct, Map.of());
    }

    /** Numbers turns from 1, each stamped with currentAct — see restored() to rebuild a window from persisted numbers. */
    public ChatSession(ChatScenario scenario, List<ChatTurn> turns, String summary, int currentAct,
                        Map<String, ForcedActivity> forced) {
        this(scenario, 1, entriesOf(turns, currentAct), summary, currentAct, forced);
    }

    private ChatSession(ChatScenario scenario, int firstTurnNumber, List<Entry> entries, String summary,
                         int currentAct, Map<String, ForcedActivity> forced) {
        this.scenario        = scenario;
        this.authoredLanguage = scenario.language();
        this.firstTurnNumber = firstTurnNumber;
        this.entries         = new ArrayList<>(entries);
        this.summary         = summary == null ? "" : summary;
        this.currentAct      = currentAct;
        this.forced.putAll(forced);
        dropStaleForced();
    }

    /**
     * A session rebuilt from persisted turns : turns must be consecutive and start exactly at
     * firstTurnNumber (an empty window is allowed — a compaction that folded everything), otherwise
     * the persisted files are inconsistent and loading fails loudly instead of silently renumbering.
     */
    public static ChatSession restored(ChatScenario scenario, String summary, TurnNumber firstTurnNumber,
                                        List<NumberedTurn> turns, int currentAct, Map<String, ForcedActivity> forced) {
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
        return new ChatSession(scenario, firstTurnNumber.value(), entries, summary, currentAct, forced);
    }

    private static List<Entry> entriesOf(List<ChatTurn> turns, int act) {
        return turns.stream().map(turn -> new Entry(turn, act)).toList();
    }

    private static Set<String> allNpcIds(ChatScenario scenario) {
        return scenario.cast().npcs().stream().map(Npc::id).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Forced levels equivalent to an old "present" list (state.txt before the activity levels) : whoever
     * is not in it is forced ABSENT from the root, so an old run reopens with the same scene.
     */
    public static Map<String, ForcedActivity> forcedAbsentFrom(ChatScenario scenario, Set<String> presentNpcIds) {
        Map<String, ForcedActivity> forced = new LinkedHashMap<>();
        for (String id : allNpcIds(scenario)) {
            if (!presentNpcIds.contains(id)) forced.put(id, new ForcedActivity(NpcActivity.ABSENT, ""));
        }
        return forced;
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

    /**
     * The language the player picked for this game, or null for "Auto" (the scenario's own). It applies from
     * the next reply on ; the turns already written stay in the former language. Every agent reads
     * scenario().language(), so none needs to know about the choice.
     */
    public void chooseLanguage(ScenarioLanguage language) {
        this.chosenLanguage = language;
        applyChosenLanguage();
    }

    /** The player's pick, null for "Auto". */
    public ScenarioLanguage chosenLanguage() { return chosenLanguage; }

    /** The language the scenario file itself declares (what "Auto" means). */
    public ScenarioLanguage authoredLanguage() { return authoredLanguage; }

    private void applyChosenLanguage() {
        scenario = scenario.withLanguage(chosenLanguage != null ? chosenLanguage : authoredLanguage);
    }

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

    private StyleReceived lastStyleReceived;

    /** Remembers the style the last reply was given (debug 🐞, never persisted). */
    public void rememberStyleReceived(StyleReceived received) { this.lastStyleReceived = received; }

    /** The style of the last reply that was generated in this run, empty before any reply. */
    public Optional<StyleReceived> lastStyleReceived() { return Optional.ofNullable(lastStyleReceived); }

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

    /**
     * The activity of every Npc right now : what the scenario's commands give at the current act,
     * overlaid with the levels the player forced that still hold (see cycleActivity). Computed on
     * demand, never stored, so a rollback, a save or a reload need no special handling.
     */
    public CastActivity activity() {
        CastActivity activity = scenarioActivity();
        for (Map.Entry<String, ForcedActivity> entry : forced.entrySet()) {
            if (!holdsHere(entry.getValue()) || activity.levelOf(entry.getKey()).isOutside()) continue; // never brings anyone back into the story
            activity = activity.with(entry.getKey(), new NpcActivityState(entry.getValue().level(), ActivitySource.FORCED));
        }
        // Un forcage ne doit jamais vider la scene : quelqu'un doit pouvoir repondre.
        return activity.inSceneIds().isEmpty() ? scenarioActivity() : activity;
    }

    /** The activity the scenario alone gives at the current act, without the player's forced levels. */
    public CastActivity scenarioActivity() { return scenario.activityAt(currentAct); }

    /** The characters with `mode` switched on in the current act (see ChatScenario.npcIdsInMode). */
    public Set<String> npcIdsInMode(NpcMode mode) { return scenario.npcIdsInMode(currentAct, mode); }

    /**
     * True if the pending opening is a DO (the opening Npc holds the narrator's pen) : "{npc-do}", or
     * "{npc-starts Name}" for a character whose DO mode is on (the first named decides when there are
     * several, they all share the round). A nameless "{npc-starts}" stays a normal opening : the
     * character is only drawn when it speaks.
     */
    public boolean pendingOpeningUsesPen() {
        NpcOpening opening = pendingNpcOpening();
        if (opening.isNone()) return false;
        if (opening.doPen()) return true;
        return ActivityResolver.openingNpcs(scenario.cast(), opening).stream().findFirst()
            .map(npc -> npcIdsInMode(NpcMode.DO).contains(npc.id())).orElse(false);
    }

    /** Ids of the Npcs in the scene (every level but ABSENT and OUTSIDE). */
    public Set<String> presentNpcIds() { return activity().inSceneIds(); }

    /** Ids of the Npcs outside the story right now (see NpcActivity.OUTSIDE) : not shown, not in anyone's prompt. */
    public Set<String> outsideNpcIds() { return activity().outsideIds(); }

    /** The forced levels still holding at the current act, by npc id — what gets persisted. */
    public Map<String, ForcedActivity> forcedActivities() {
        Map<String, ForcedActivity> holding = new LinkedHashMap<>();
        forced.forEach((id, f) -> { if (holdsHere(f)) holding.put(id, f); });
        return holding;
    }

    /**
     * Steps one Cast member to the next forced level (a click). From the level the scenario gives, it
     * goes through the other levels in the order active, present, passive, absent, then through the
     * scenario's own level forced, and a last click removes the forcing. A forced level hangs from the
     * parent heading of the current act and disappears when the story leaves it. A step that would
     * leave nobody in the scene is skipped. Returns false (no-op) for an id the Cast does not know.
     */
    public boolean cycleActivity(String npcId) {
        if (scenario.cast().find(npcId).isEmpty()) return false;
        NpcActivity base = scenarioActivity().levelOf(npcId);
        if (base.isOutside()) return false; // not in the story : there is no vignette to click
        List<NpcActivity> steps = List.of(base.next(), base.next().next(), base.next().next().next(), base);
        ForcedActivity current = forced.get(npcId) != null && holdsHere(forced.get(npcId)) ? forced.get(npcId) : null;
        int position = current == null ? -1 : steps.indexOf(current.level());
        for (int next = position + 1; next < steps.size(); next++) {
            if (steps.get(next) == NpcActivity.ABSENT && leavesNobody(npcId)) continue;
            forced.put(npcId, new ForcedActivity(steps.get(next), current != null ? current.anchor() : parentAnchor()));
            return true;
        }
        return forced.remove(npcId) != null;
    }

    private boolean leavesNobody(String npcId) {
        return activity().inSceneIds().stream().noneMatch(id -> !id.equals(npcId));
    }

    private String parentAnchor() {
        return currentAct >= 1 && currentAct <= scenario.acts().size() ? scenario.acts().get(currentAct - 1).parentAnchor() : "";
    }

    /** A forced level holds while the current act is under the heading it hangs from (the root holds everywhere). */
    private boolean holdsHere(ForcedActivity f) {
        if (f.anchor().isEmpty()) return true;
        return currentAct >= 1 && currentAct <= scenario.acts().size() && scenario.acts().get(currentAct - 1).isUnder(f.anchor());
    }

    /** Leaving the heading a forced level hangs from ends it for good : coming back later does not revive it. */
    private void dropStaleForced() {
        forced.values().removeIf(f -> !holdsHere(f));
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
        dropStaleForced();
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
        dropStaleForced();
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
     * Replaces turns/summary/currentAct/forced levels in place with fresh content (a fresh or
     * restarted run), numbering the turns from 1 — the ChatSession instance itself is kept
     * (callers, e.g. ChatWebServer, hold a single long-lived reference), only its state changes.
     * generationSettings is deliberately untouched : session-only tuning knob, see GenerationSettings.
     */
    public void restore(String summary, List<ChatTurn> turns, int currentAct, Map<String, ForcedActivity> forced) {
        this.summary = summary == null ? "" : summary;
        this.entries.clear();
        this.entries.addAll(entriesOf(turns, currentAct));
        this.firstTurnNumber = 1;
        this.currentAct = currentAct;
        this.forced.clear();
        this.forced.putAll(forced);
        dropStaleForced();
    }

    /**
     * Replaces the whole run state in place with other's — window with its numbers, summary, act and
     * forced levels — for loading a save point. generationSettings stays untouched, as for restore().
     */
    public void restoreFrom(ChatSession other) {
        restoreConversationFrom(other);
        this.forced.clear();
        this.forced.putAll(other.forced);
        dropStaleForced();
    }

    /**
     * Replaces only the conversation (window with its numbers, summary, current act) with other's,
     * leaving the player's forced levels untouched (except those that no longer hold at the act it
     * lands on) — a rollback rewinds what was said, never the
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
        dropStaleForced();
    }

    private static boolean sameContent(ChatTurn a, ChatTurn b) {
        return a.speaker() == b.speaker()
            && java.util.Objects.equals(a.npcId(), b.npcId())
            && a.text().strip().equals(b.text().strip());
    }

    /**
     * Re-reads the scenario/cast from disk into this session, without touching turns/summary/
     * currentAct — for editing scenario.txt or a character's .txt file while a session is running
     * and picking up the change without losing the conversation so far. The player's forced levels
     * are reconciled against the new cast : those of Npc ids removed from the cast are dropped, and so
     * are those that no longer hold under the new act tree. A newly added Npc simply gets the
     * scenario's activity for it.
     */
    public void reloadScenario(ChatScenario freshScenario) {
        Set<String> freshIds = allNpcIds(freshScenario);
        this.authoredLanguage = freshScenario.language();
        this.scenario = freshScenario;
        applyChosenLanguage();
        forced.keySet().retainAll(freshIds);
        dropStaleForced();
    }
}
