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
import storymagine.chat.coeur.domaine.session.ChatContextBudget;
import storymagine.chat.coeur.domaine.session.ChatPromptBuilder;
import storymagine.chat.coeur.domaine.session.ChatPromptBuilder.ChatPrompt;
import storymagine.chat.coeur.domaine.session.ChatSession;
import storymagine.chat.coeur.domaine.session.ChatTurn;
import storymagine.chat.coeur.domaine.session.PlayerMessage;
import storymagine.chat.coeur.domaine.session.SavePoint;
import storymagine.chat.coeur.ports.ChatStoragePort;
import storymagine.commun.coeur.ports.GenerationCancelledException;
import storymagine.commun.coeur.ports.LogPort;

import java.nio.file.Path;
import java.util.List;

/**
 * Orchestrates one exchange : calls RoleplayNarrator to play the character for one turn (see
 * RoleplayNarrator.md for the prompt design), appends both turns, then folds older turns into the
 * summary once the live transcript outgrows its estimated token budget (see ChatContextBudget).
 * See ChatSummarizer.md for the compaction rationale.
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

    public ChatServiceImpl(ChatStoragePort storage, RoleplayNarrator roleplayNarrator, ChatSummarizer summarizer,
                            NextActReadinessAnalyst nextActReadinessAnalyst, NpcMindStateAnalyst npcMindStateAnalyst) {
        this(storage, roleplayNarrator, summarizer, nextActReadinessAnalyst, npcMindStateAnalyst, LogPort.NOOP);
    }

    public ChatServiceImpl(ChatStoragePort storage, RoleplayNarrator roleplayNarrator, ChatSummarizer summarizer,
                            NextActReadinessAnalyst nextActReadinessAnalyst, NpcMindStateAnalyst npcMindStateAnalyst,
                            LogPort log) {
        this.storage                 = storage;
        this.roleplayNarrator        = roleplayNarrator;
        this.summarizer              = summarizer;
        this.nextActReadinessAnalyst = nextActReadinessAnalyst;
        this.npcMindStateAnalyst     = npcMindStateAnalyst;
        this.log                     = log;
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
        PlayerMessage input = PlayerMessage.parse(rawPlayerInput);
        ChatTurn playerTurn = new ChatTurn(ChatTurn.Speaker.PLAYER, input.formattedLine());
        session.append(playerTurn);
        int playerTurnIndex = session.turns().size() - 1;
        try {
            return generateReplyAndFinish(chatScenariosRoot, session, playerTurn, input, 0);
        } catch (GenerationCancelledException e) {
            // Retire le tour joueur en attente : rien n'a ete montre cote serveur pour cet
            // echange, le joueur doit pouvoir corriger et renvoyer son message sans qu'un tour
            // orphelin ne traine dans l'historique (voir ChatWebServer.handleMessage).
            session.truncateFrom(playerTurnIndex);
            throw e;
        }
    }

    @Override
    public ChatTurnResult retry(Path chatScenariosRoot, ChatSession session) {
        List<ChatTurn> turns = session.turns();
        if (turns.size() < 2 || turns.get(turns.size() - 1).speaker() != ChatTurn.Speaker.LLM) {
            throw new IllegalStateException(
                "Rien à régénérer : le dernier tour n'est pas une réponse du personnage.");
        }
        ChatTurn playerTurn = turns.get(turns.size() - 2);
        session.truncateFrom(turns.size() - 1); // retire l'ancienne reponse, garde le tour joueur
        PlayerMessage input = PlayerMessage.parse(playerTurn.text());
        return generateReplyAndFinish(chatScenariosRoot, session, playerTurn, input, 1);
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
     * nothing after it) ; calls the LLM with everything BEFORE that turn as context, appends the
     * reply, handles a possible act advance, compaction and persistence. replacedTurnCount is
     * threaded straight into the result — see ChatTurnResult.
     */
    private ChatTurnResult generateReplyAndFinish(Path chatScenariosRoot, ChatSession session, ChatTurn playerTurn,
                                                    PlayerMessage input, int replacedTurnCount) {
        List<ChatTurn> allTurns    = session.turns();
        List<ChatTurn> recentTurns = allTurns.subList(0, allTurns.size() - 1);

        RoleplayNarratorOutput reply = roleplayNarrator.call(new RoleplayNarratorInput(
            session.scenario(), session.currentAct(), session.summary(), recentTurns, input,
            session.generationSettings()));

        ChatTurn replyTurn = new ChatTurn(ChatTurn.Speaker.LLM, reply.replyText(), reply.thinking());
        session.append(replyTurn);

        int sizeBeforeAdvance = session.turns().size();
        boolean actAdvanced = reply.triggeredNextAct() && session.advanceAct();
        List<ChatTurn> narratorTurnsFromAdvance = actAdvanced
            ? session.turns().subList(sizeBeforeAdvance, session.turns().size())
            : List.of();
        boolean compacted = compactIfNeeded(chatScenariosRoot, session);
        storage.saveSession(chatScenariosRoot, session);
        return new ChatTurnResult(playerTurn, replyTurn, compacted, currentPromptEstimate(session), actAdvanced,
            narratorTurnsFromAdvance, replacedTurnCount);
    }

    /**
     * Estimated size (chars/4, see ChatContextBudget) of the prompt the NEXT turn would send,
     * computed locally from the session's state right now — after this turn's append and any
     * compaction — so the gauge reflects reality immediately instead of the size Ollama measured
     * for the call that just happened, which predates this turn's two new turns and any fold-in.
     */
    private int currentPromptEstimate(ChatSession session) {
        ChatPrompt next = ChatPromptBuilder.build(session.scenario(), session.currentAct(), session.summary(),
            session.turns(), PlayerMessage.parse(""));
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
        ChatPrompt withoutTurns = ChatPromptBuilder.build(session.scenario(), session.currentAct(), session.summary(),
            List.of(), PlayerMessage.parse(""));
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
            session.scenario(), currentAct, session.summary(), session.turns(), session.generationSettings()));
    }

    @Override
    public NpcMindStateAnalystOutput analyzeMindState(Path chatScenariosRoot, ChatSession session) {
        return npcMindStateAnalyst.call(new NpcMindStateAnalystInput(
            session.scenario(), session.currentAct(), session.summary(), session.turns(), session.generationSettings()));
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
        session.restore(loaded.summary(), loaded.turns(), loaded.currentAct());
        storage.saveSession(chatScenariosRoot, session);
    }

    @Override
    public void deleteSavePoint(Path chatScenariosRoot, ChatScenario scenario, String saveId) {
        storage.deleteSavePoint(chatScenariosRoot, scenario, saveId);
    }

    @Override
    public int contextWindow() {
        return roleplayNarrator.contextWindow();
    }

    private boolean compactIfNeeded(Path chatScenariosRoot, ChatSession session) {
        List<ChatTurn> turns = session.turns();
        if (turns.size() <= KEEP_RECENT_TURNS) return false;

        String characterLabel = ChatPromptBuilder.characterLabel(session.scenario());
        int threshold = ChatContextBudget.turnsBudget(roleplayNarrator.contextWindow(), fixedPartsEstimate(session));
        if (ChatContextBudget.estimateTokens(ChatPromptBuilder.transcript(turns, characterLabel)) <= threshold) return false;

        List<ChatTurn> toFold = turns.subList(0, turns.size() - KEEP_RECENT_TURNS);
        List<ChatTurn> toKeep = turns.subList(turns.size() - KEEP_RECENT_TURNS, turns.size());

        String newSummary = summarizer.call(
            new ChatSummarizerInput(session.summary(), ChatPromptBuilder.transcript(toFold, characterLabel),
                characterLabel)).summary();
        if (newSummary == null || newSummary.isBlank()) {
            log.warn("ChatSummarizer a renvoyé un résumé vide — compactage ignoré ce tour-ci, historique conservé");
            return false;
        }
        storage.archiveFoldedTurns(chatScenariosRoot, session.scenario(), toFold);
        session.compactInto(newSummary, toKeep);
        return true;
    }

}
