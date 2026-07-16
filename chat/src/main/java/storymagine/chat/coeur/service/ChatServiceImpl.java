package storymagine.chat.coeur.service;

import storymagine.chat.coeur.domaine.agent.chatsummarizer.ChatSummarizer;
import storymagine.chat.coeur.domaine.agent.chatsummarizer.ChatSummarizerInput;
import storymagine.chat.coeur.domaine.agent.nextactreadiness.NextActReadinessAnalyst;
import storymagine.chat.coeur.domaine.agent.nextactreadiness.NextActReadinessAnalystInput;
import storymagine.chat.coeur.domaine.agent.nextactreadiness.NextActReadinessAnalystOutput;
import storymagine.chat.coeur.domaine.agent.npcmindstate.NpcMindStateAnalyst;
import storymagine.chat.coeur.domaine.agent.npcmindstate.NpcMindStateAnalystInput;
import storymagine.chat.coeur.domaine.agent.npcmindstate.NpcMindStateAnalystOutput;
import storymagine.chat.coeur.domaine.agent.roleplaynarrator.RoleplayNarrator;
import storymagine.chat.coeur.domaine.agent.roleplaynarrator.RoleplayNarratorInput;
import storymagine.chat.coeur.domaine.agent.roleplaynarrator.RoleplayNarratorOutput;
import storymagine.chat.coeur.domaine.scenario.ChatScenario;
import storymagine.chat.coeur.domaine.scenario.Npc;
import storymagine.chat.coeur.domaine.session.ChatContextBudget;
import storymagine.chat.coeur.domaine.session.ChatPromptBuilder;
import storymagine.chat.coeur.domaine.session.ChatPromptBuilder.ChatPrompt;
import storymagine.chat.coeur.domaine.session.ChatSession;
import storymagine.chat.coeur.domaine.session.ChatTurn;
import storymagine.chat.coeur.domaine.session.PlayerMessage;
import storymagine.chat.coeur.domaine.session.SavePoint;
import storymagine.chat.coeur.domaine.session.Scene;
import storymagine.chat.coeur.domaine.session.SpeakerSelector;
import storymagine.chat.coeur.ports.ChatStoragePort;
import storymagine.commun.coeur.ports.GenerationCancelledException;
import storymagine.commun.coeur.ports.LogPort;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

/**
 * Orchestrates one exchange : runs SpeakerSelector to pick which Cast member(s) answer the
 * player's line, calls RoleplayNarrator once per selected Npc in turn — each seeing the previous
 * one's reply, so a multi-Npc round reads as a real conversation, not parallel monologues — then
 * folds older turns into the summary once the live transcript outgrows its estimated token budget
 * (see ChatContextBudget). See RoleplayNarrator.md for the prompt design, ChatSummarizer.md for
 * the compaction rationale, and the multi-NPC plan (project memory / evols) for SpeakerSelector.
 */
public class ChatServiceImpl implements ChatService {

    /** Turns kept verbatim after a compaction (2 exchanges) — the rest folds into the summary. */
    private static final int KEEP_RECENT_TURNS = 4;

    private final ChatStoragePort            storage;
    private final RoleplayNarrator           roleplayNarrator;
    private final ChatSummarizer             summarizer;
    private final NextActReadinessAnalyst    nextActReadinessAnalyst;
    private final NpcMindStateAnalyst        npcMindStateAnalyst;
    private final LogPort                    log;
    private final Random                     random;

    public ChatServiceImpl(ChatStoragePort storage, RoleplayNarrator roleplayNarrator, ChatSummarizer summarizer,
                            NextActReadinessAnalyst nextActReadinessAnalyst, NpcMindStateAnalyst npcMindStateAnalyst) {
        this(storage, roleplayNarrator, summarizer, nextActReadinessAnalyst, npcMindStateAnalyst, LogPort.NOOP);
    }

    public ChatServiceImpl(ChatStoragePort storage, RoleplayNarrator roleplayNarrator, ChatSummarizer summarizer,
                            NextActReadinessAnalyst nextActReadinessAnalyst, NpcMindStateAnalyst npcMindStateAnalyst,
                            LogPort log) {
        this(storage, roleplayNarrator, summarizer, nextActReadinessAnalyst, npcMindStateAnalyst, log, new Random());
    }

    /** random is a test seam (SpeakerSelector's no-mention fallback) — real wiring never needs to pass it explicitly. */
    public ChatServiceImpl(ChatStoragePort storage, RoleplayNarrator roleplayNarrator, ChatSummarizer summarizer,
                            NextActReadinessAnalyst nextActReadinessAnalyst, NpcMindStateAnalyst npcMindStateAnalyst,
                            LogPort log, Random random) {
        this.storage                 = storage;
        this.roleplayNarrator        = roleplayNarrator;
        this.summarizer              = summarizer;
        this.nextActReadinessAnalyst = nextActReadinessAnalyst;
        this.npcMindStateAnalyst     = npcMindStateAnalyst;
        this.log                     = log;
        this.random                  = random;
    }

    @Override
    public List<String> listScenarios(Path chatScenariosRoot) {
        return storage.listScenarioNames(chatScenariosRoot);
    }

    @Override
    public ChatScenario loadScenario(Path chatScenariosRoot, String name) {
        return storage.loadScenario(chatScenariosRoot, name);
    }

    @Override
    public ChatSession openSession(Path chatScenariosRoot, ChatScenario scenario, boolean reset) {
        if (reset) {
            storage.resetSession(chatScenariosRoot, scenario);
            return ChatSession.fresh(scenario);
        }
        return storage.loadSession(chatScenariosRoot, scenario);
    }

    @Override
    public ChatTurnResult sendMessage(Path chatScenariosRoot, ChatSession session, String rawPlayerInput) {
        return sendMessage(chatScenariosRoot, session, rawPlayerInput, turn -> {});
    }

    @Override
    public ChatTurnResult sendMessage(Path chatScenariosRoot, ChatSession session, String rawPlayerInput,
                                       Consumer<ChatTurn> onReplyReady) {
        PlayerMessage input = PlayerMessage.parse(rawPlayerInput);
        ChatTurn playerTurn = new ChatTurn(ChatTurn.Speaker.PLAYER, input.formattedLine());
        session.append(playerTurn);
        onReplyReady.accept(playerTurn);
        int playerTurnIndex = session.turns().size() - 1;
        try {
            List<RoundSpeaker> speakers = resolveSpeakers(session, input.formattedLine());
            return generateRepliesAndFinish(chatScenariosRoot, session, playerTurn, speakers, 0, onReplyReady);
        } catch (GenerationCancelledException e) {
            // Retire le tour joueur en attente : rien n'a ete montre cote serveur pour cet
            // echange, le joueur doit pouvoir corriger et renvoyer son message sans qu'un tour
            // orphelin ne traine dans l'historique (voir ChatWebServer.handleMessage).
            session.truncateFrom(playerTurnIndex);
            throw e;
        }
    }

    /**
     * Regenerates every reply since the last player turn — a "round" can be more than one Npc now
     * (see SpeakerSelector), so this drops the whole trailing run of LLM/NARRATOR turns after that
     * player line, not just a single last turn. Re-runs SpeakerSelector fresh : a retry can pick a
     * different set of Npcs than the round it replaces (accepted simplification, see the multi-NPC
     * plan — tracking and forcing the exact same speakers back would need extra state for little
     * practical benefit).
     */
    @Override
    public ChatTurnResult retry(Path chatScenariosRoot, ChatSession session) {
        return retry(chatScenariosRoot, session, null);
    }

    /**
     * fromNpcId == null : regenerates the whole round, re-running SpeakerSelector fresh — a retry
     * can then pick a different set of Npcs than the round it replaces (accepted simplification,
     * see the multi-NPC plan). fromNpcId != null : keeps every reply strictly before that Npc's
     * turn in the current round untouched, and replays only that Npc and whichever ones originally
     * followed it — same identities, same primary/interjecting role, never re-selected (see
     * ChatService.retry(fromNpcId) javadoc for why a fresh selection would be wrong here).
     */
    @Override
    public ChatTurnResult retry(Path chatScenariosRoot, ChatSession session, String fromNpcId) {
        List<ChatTurn> turns = session.turns();
        int lastPlayerIdx = lastPlayerTurnIndex(turns);
        if (lastPlayerIdx < 0 || lastPlayerIdx == turns.size() - 1) {
            throw new IllegalStateException(
                "Rien à régénérer : le dernier tour n'est pas une réponse du personnage.");
        }
        ChatTurn playerTurn = turns.get(lastPlayerIdx);

        if (fromNpcId == null) {
            int replacedCount = turns.size() - (lastPlayerIdx + 1);
            session.truncateFrom(lastPlayerIdx + 1); // retire tout ce qui suit ce tour joueur, le garde lui
            List<RoundSpeaker> speakers = resolveSpeakers(session, playerTurn.text());
            return generateRepliesAndFinish(chatScenariosRoot, session, playerTurn, speakers, replacedCount, turn -> {});
        }

        List<RoundSpeaker> toRegenerate = new ArrayList<>();
        int fromIdx = -1;
        boolean sawPrimary = false;
        for (int i = lastPlayerIdx + 1; i < turns.size(); i++) {
            ChatTurn t = turns.get(i);
            if (t.speaker() != ChatTurn.Speaker.LLM) continue;
            boolean isPrimary = !sawPrimary;
            sawPrimary = true;
            if (fromIdx < 0 && fromNpcId.equals(t.npcId())) fromIdx = i;
            if (fromIdx >= 0) {
                Npc npc = session.scenario().cast().find(t.npcId())
                    .orElseThrow(() -> new IllegalStateException("PNJ inconnu : " + t.npcId()));
                toRegenerate.add(new RoundSpeaker(npc, !isPrimary));
            }
        }
        if (fromIdx < 0) {
            throw new IllegalStateException("Ce PNJ n'a pas répondu dans ce tour.");
        }
        int replacedCount = turns.size() - fromIdx;
        session.truncateFrom(fromIdx);
        return generateRepliesAndFinish(chatScenariosRoot, session, playerTurn, toRegenerate, replacedCount, turn -> {});
    }

    private static int lastPlayerTurnIndex(List<ChatTurn> turns) {
        for (int i = turns.size() - 1; i >= 0; i--) {
            if (turns.get(i).speaker() == ChatTurn.Speaker.PLAYER) return i;
        }
        return -1;
    }

    @Override
    public int undo(Path chatScenariosRoot, ChatSession session, int steps) {
        List<ChatTurn> turns = session.turns();
        int targetIdx = -1;
        int remaining = Math.max(1, steps);
        for (int i = turns.size() - 1; i >= 0; i--) {
            if (turns.get(i).speaker() == ChatTurn.Speaker.PLAYER) {
                targetIdx = i;
                if (--remaining == 0) break;
            }
        }
        if (targetIdx < 0) return 0;
        int removedCount = turns.size() - targetIdx;
        session.truncateFrom(targetIdx);
        storage.saveSession(chatScenariosRoot, session);
        return removedCount;
    }

    /**
     * Shared tail of sendMessage/retry : session already has the player's turn appended (and
     * nothing after it). Calls RoleplayNarrator once per speaker, in order — each call reads
     * session.turns() fresh, so speaker 2+ sees speaker 1's reply already in the transcript, not
     * just the player's line. Only the first [NEXT ACT] trigger in the round is honoured (a later
     * speaker's own trigger in the same round is ignored — a rare edge case, not worth a second act
     * advance mid-round). replacedTurnCount is threaded straight into the result — see ChatTurnResult.
     */
    /** One speaker's turn in a round : npc plus whether they're reacting unprompted (see SpeakerSelector). */
    private record RoundSpeaker(Npc npc, boolean interjecting) {}

    private ChatTurnResult generateRepliesAndFinish(Path chatScenariosRoot, ChatSession session, ChatTurn playerTurn,
                                                      List<RoundSpeaker> speakers, int replacedTurnCount,
                                                      Consumer<ChatTurn> onReplyReady) {
        List<ChatTurn> replyTurns = new ArrayList<>();
        boolean actAdvanced = false;
        List<ChatTurn> narratorTurnsFromAdvance = List.of();

        for (RoundSpeaker round : speakers) {
            Scene scene = sceneFor(session, round.npc(), round.interjecting());
            RoleplayNarratorOutput reply = roleplayNarrator.call(new RoleplayNarratorInput(
                session.scenario(), scene, session.currentAct(), session.summary(), session.turns(),
                session.generationSettings()));

            ChatTurn replyTurn = new ChatTurn(ChatTurn.Speaker.LLM, reply.replyText(), reply.thinking(), round.npc().id());
            session.append(replyTurn);
            replyTurns.add(replyTurn);
            onReplyReady.accept(replyTurn);

            if (reply.triggeredNextAct() && !actAdvanced) {
                int sizeBeforeAdvance = session.turns().size();
                actAdvanced = session.advanceAct();
                if (actAdvanced) {
                    narratorTurnsFromAdvance = session.turns().subList(sizeBeforeAdvance, session.turns().size());
                    for (ChatTurn narratorTurn : narratorTurnsFromAdvance) onReplyReady.accept(narratorTurn);
                }
            }
        }

        boolean compacted = compactIfNeeded(chatScenariosRoot, session);
        storage.saveSession(chatScenariosRoot, session);
        return new ChatTurnResult(playerTurn, List.copyOf(replyTurns), compacted, currentPromptEstimate(session),
            actAdvanced, narratorTurnsFromAdvance, replacedTurnCount);
    }

    /**
     * Combines SpeakerSelector's mention/fallback pick with the independent interjection rolls
     * (see SpeakerSelector.rollInterjectors) into one ordered round : primary speaker(s) first,
     * interjectors after — an interjector's prompt sees the primary's reply already in the
     * transcript, matching the "react to what was just said" framing in INTERJECTION_RULE.
     */
    private List<RoundSpeaker> resolveSpeakers(ChatSession session, String playerMessage) {
        List<Npc> primary = SpeakerSelector.select(session.scenario().cast(), session.presentNpcIds(),
            session.interjectingNpcIds(), playerMessage, random);
        List<Npc> interjectors = SpeakerSelector.rollInterjectors(session.scenario().cast(), session.presentNpcIds(),
            session.interjectingNpcIds(), primary, playerMessage, interjectionChance(session), random);

        List<RoundSpeaker> round = new ArrayList<>();
        for (Npc npc : primary) round.add(new RoundSpeaker(npc, false));
        for (Npc npc : interjectors) round.add(new RoundSpeaker(npc, true));
        return round;
    }

    private static double interjectionChance(ChatSession session) {
        Double configured = session.generationSettings().interjectionChance();
        return configured != null ? configured : RoleplayNarrator.INTERJECTION_CHANCE_DEFAULT;
    }

    /** speaker gets every other present Npc, minus themselves — see Scene, ChatPromptBuilder. */
    private static Scene sceneFor(ChatSession session, Npc speaker, boolean interjecting) {
        var present = session.presentNpcIds();
        List<Npc> otherPresent = session.scenario().cast().npcs().stream()
            .filter(n -> present.contains(n.id()) && !n.id().equals(speaker.id()))
            .toList();
        return new Scene(speaker, otherPresent, interjecting);
    }

    /** The first present Npc, standing in for "who's speaking" when only a prompt SIZE estimate is needed. */
    private static Scene representativeScene(ChatSession session) {
        List<Npc> present = session.scenario().cast().npcs().stream()
            .filter(n -> session.presentNpcIds().contains(n.id()))
            .toList();
        if (present.isEmpty()) {
            throw new IllegalStateException("Aucun personnage present dans ce scenario.");
        }
        return new Scene(present.get(0), present.subList(1, present.size()), false);
    }

    /**
     * Estimated size (chars/4, see ChatContextBudget) of the prompt the NEXT turn would send,
     * computed locally from the session's state right now — after this turn's append and any
     * compaction — so the gauge reflects reality immediately instead of the size Ollama measured
     * for the call that just happened, which predates this turn's two new turns and any fold-in.
     */
    private int currentPromptEstimate(ChatSession session) {
        ChatPrompt next = ChatPromptBuilder.build(session.scenario(), representativeScene(session),
            session.currentAct(), session.summary(), session.turns());
        return ChatContextBudget.estimateTokens(next.system() + next.user());
    }

    /**
     * Estimated size of everything BUT the raw turns — character sheet, scenario, current act,
     * summary, formatting rules — built with an empty turn list so "Recent exchange:" is omitted
     * (see ChatPromptBuilder). Used to size the turns-only compaction budget dynamically instead of
     * a flat fraction, so a big character sheet, a long act or a growing summary make it compact
     * sooner rather than silently pushing the total prompt past the context window.
     */
    private int fixedPartsEstimate(ChatSession session) {
        ChatPrompt withoutTurns = ChatPromptBuilder.build(session.scenario(), representativeScene(session),
            session.currentAct(), session.summary(), List.of());
        return ChatContextBudget.estimateTokens(withoutTurns.system() + withoutTurns.user());
    }

    @Override
    public boolean advanceAct(Path chatScenariosRoot, ChatSession session) {
        boolean advanced = session.advanceAct();
        if (advanced) storage.saveSession(chatScenariosRoot, session);
        return advanced;
    }

    @Override
    public boolean previousAct(Path chatScenariosRoot, ChatSession session) {
        boolean movedBack = session.previousAct();
        if (movedBack) storage.saveSession(chatScenariosRoot, session);
        return movedBack;
    }

    @Override
    public NextActReadinessAnalystOutput analyzeNextActReadiness(Path chatScenariosRoot, ChatSession session) {
        int currentAct = session.currentAct();
        if (currentAct <= 0 || currentAct >= session.scenario().acts().size()) {
            throw new IllegalStateException("Pas d'acte suivant à analyser.");
        }
        return nextActReadinessAnalyst.call(new NextActReadinessAnalystInput(
            session.scenario(), representativeScene(session).speaker(), currentAct, session.summary(),
            session.turns(), session.generationSettings()));
    }

    @Override
    public NpcMindStateAnalystOutput analyzeMindState(Path chatScenariosRoot, ChatSession session) {
        return npcMindStateAnalyst.call(new NpcMindStateAnalystInput(
            session.scenario(), representativeScene(session).speaker(), session.currentAct(), session.summary(),
            session.turns(), session.generationSettings()));
    }

    @Override
    public SavePoint save(Path chatScenariosRoot, ChatSession session) {
        return storage.createSavePoint(chatScenariosRoot, session);
    }

    @Override
    public List<SavePoint> listSavePoints(Path chatScenariosRoot, ChatScenario scenario) {
        return storage.listSavePoints(chatScenariosRoot, scenario);
    }

    @Override
    public void loadSavePoint(Path chatScenariosRoot, ChatSession session, String saveId) {
        // Pas de sauvegarde de securite automatique ici : elle surprenait plus qu'elle ne
        // rassurait (une entree apparaissait dans la liste sans explication). Le client demande
        // confirmation avant d'appeler ce point d'entree (voir chat.html) — l'ecrasement est donc
        // toujours un choix explicite du joueur, pas un accident a rattraper.
        ChatSession loaded = storage.loadSavePoint(chatScenariosRoot, session.scenario(), saveId);
        session.restore(loaded.summary(), loaded.turns(), loaded.currentAct(), loaded.presentNpcIds(),
            loaded.interjectingNpcIds());
        storage.saveSession(chatScenariosRoot, session);
    }

    @Override
    public void deleteSavePoint(Path chatScenariosRoot, ChatScenario scenario, String saveId) {
        storage.deleteSavePoint(chatScenariosRoot, scenario, saveId);
    }

    @Override
    public boolean setNpcPresent(Path chatScenariosRoot, ChatSession session, String npcId, boolean present) {
        boolean changed = session.setPresent(npcId, present);
        if (changed) storage.saveSession(chatScenariosRoot, session);
        return changed;
    }

    @Override
    public boolean setNpcInterjecting(Path chatScenariosRoot, ChatSession session, String npcId, boolean interjecting) {
        boolean changed = session.setInterjecting(npcId, interjecting);
        if (changed) storage.saveSession(chatScenariosRoot, session);
        return changed;
    }

    @Override
    public void reloadScenario(Path chatScenariosRoot, ChatSession session) {
        ChatScenario fresh = storage.loadScenario(chatScenariosRoot, session.scenario().name());
        session.reloadScenario(fresh);
        storage.saveSession(chatScenariosRoot, session);
    }

    @Override
    public void restartSession(Path chatScenariosRoot, ChatSession session) {
        storage.resetSession(chatScenariosRoot, session.scenario());
        ChatSession fresh = ChatSession.fresh(session.scenario());
        session.restore(fresh.summary(), fresh.turns(), fresh.currentAct(), fresh.presentNpcIds(),
            fresh.interjectingNpcIds());
        storage.saveSession(chatScenariosRoot, session);
    }

    @Override
    public int contextWindow() {
        return roleplayNarrator.contextWindow();
    }

    private boolean compactIfNeeded(Path chatScenariosRoot, ChatSession session) {
        List<ChatTurn> turns = session.turns();
        if (turns.size() <= KEEP_RECENT_TURNS) return false;

        int threshold = ChatContextBudget.turnsBudget(roleplayNarrator.contextWindow(), fixedPartsEstimate(session));
        String playerName = session.scenario().playerName();
        if (ChatContextBudget.estimateTokens(ChatPromptBuilder.transcript(turns, session.scenario().cast(), playerName))
                <= threshold) return false;

        List<ChatTurn> toFold = turns.subList(0, turns.size() - KEEP_RECENT_TURNS);
        List<ChatTurn> toKeep = turns.subList(turns.size() - KEEP_RECENT_TURNS, turns.size());

        String newSummary = summarizer.call(new ChatSummarizerInput(session.summary(),
            ChatPromptBuilder.transcript(toFold, session.scenario().cast(), playerName))).summary();
        if (newSummary == null || newSummary.isBlank()) {
            log.warn("ChatSummarizer a renvoyé un résumé vide — compactage ignoré ce tour-ci, historique conservé");
            return false;
        }
        storage.archiveFoldedTurns(chatScenariosRoot, session.scenario(), toFold);
        session.compactInto(newSummary, toKeep);
        return true;
    }

}
