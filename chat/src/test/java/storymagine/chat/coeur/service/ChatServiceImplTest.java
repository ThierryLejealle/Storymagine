package storymagine.chat.coeur.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import storymagine.chat.coeur.domaine.agent.chatsummarizer.ChatSummarizer;
import storymagine.chat.coeur.domaine.agent.nextactreadiness.NextActReadinessAnalyst;
import storymagine.chat.coeur.domaine.agent.nextactreadiness.NextActReadinessAnalystOutput;
import storymagine.chat.coeur.domaine.agent.npcmindstate.NpcMindStateAnalyst;
import storymagine.chat.coeur.domaine.agent.npcmindstate.NpcMindStateAnalystOutput;
import storymagine.chat.coeur.domaine.agent.roleplaynarrator.RoleplayNarrator;
import storymagine.chat.coeur.domaine.scenario.ChatScenario;
import storymagine.chat.coeur.domaine.session.ChatContextBudget;
import storymagine.chat.coeur.domaine.session.ChatPromptBuilder;
import storymagine.chat.coeur.domaine.session.ChatSession;
import storymagine.chat.coeur.domaine.validation.ScenarioValidationException;
import storymagine.chat.coeur.domaine.session.ChatTurn;
import storymagine.chat.coeur.domaine.session.GenerationSettings;
import storymagine.chat.coeur.domaine.session.SavePoint;
import storymagine.chat.coeur.domaine.session.Scene;
import storymagine.chat.coeur.domaine.session.TurnNumber;
import storymagine.chat.coeur.domaine.agent.replyorigin.ReplyOriginAnalyst;
import storymagine.chat.coeur.domaine.agent.replyorigin.ReplyOriginAnalystOutput;
import storymagine.chat.infra.ChatFileStorageAdapter;
import storymagine.commun.coeur.ports.GenerationCancelledException;
import storymagine.commun.coeur.ports.GenerationOptions;
import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.LlmResult;
import storymagine.commun.coeur.ports.LogPort;
import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.commun.coeur.ports.PartialGeneration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pas de vrai LLM ici — ModelCallPort est un bouchon a reponse fixe (meme port pour le tour de
 *  roleplay et pour ChatSummarizer, comme dans le vrai wiring ChatModule). */
class ChatServiceImplTest {

    private static void writeScenarioFiles(Path root, String name) throws IOException {
        Path dir = root.resolve(name);
        Files.createDirectories(dir.resolve("characters"));
        Files.writeString(dir.resolve("characters/character.txt"), "# Character\nA grumpy innkeeper.", StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("scenario.txt"), "A stormy night at the inn.", StandardCharsets.UTF_8);
    }

    /** characters/elena.txt + characters/marcus.txt (pas de character.txt) — scenario multi-PNJ pour SpeakerSelector. */
    private static void writeTwoNpcScenarioFiles(Path root, String name) throws IOException {
        Path dir = root.resolve(name);
        Files.createDirectories(dir.resolve("characters"));
        Files.writeString(dir.resolve("characters/elena.txt"), "# Elena\nA sharp-tongued merchant.", StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("characters/marcus.txt"), "# Marcus\nA retired soldier.", StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("scenario.txt"), "A stormy night at the inn.", StandardCharsets.UTF_8);
    }

    /** Names of the files of a scenario's run/current/turns/ directory, sorted. */
    private static List<String> turnFileNames(Path root, String scenarioName) throws IOException {
        Path turns = root.resolve(scenarioName).resolve("run/current/turns");
        if (!Files.isDirectory(turns)) return List.of();
        try (var files = Files.list(turns)) {
            return files.map(p -> p.getFileName().toString()).sorted().toList();
        }
    }

    private ChatServiceImpl newService(int contextWindow) {
        return newService(contextWindow, 0);
    }

    private ChatServiceImpl newService(int contextWindow, int promptTokens) {
        StubModelCallPort roleplayLlm = new StubModelCallPort(contextWindow, "grunts and nods slowly", promptTokens);
        StubModelCallPort summarizerLlm = new StubModelCallPort(32_768, "FOLDED SUMMARY", 0);
        return new ChatServiceImpl(new ChatFileStorageAdapter(), new RoleplayNarrator(roleplayLlm),
            new ChatSummarizer(summarizerLlm), new NextActReadinessAnalyst(roleplayLlm), new NpcMindStateAnalyst(roleplayLlm));
    }

    // ── Pistes sur l'origine de la derniere reponse ────────────────────────────

    private ChatServiceImpl newServiceWithOriginAnalyst(RecordingModelCallPort analysisLlm) {
        StubModelCallPort roleplayLlm = new StubModelCallPort(32_768, "grunts and nods slowly", 0);
        return new ChatServiceImpl(new ChatFileStorageAdapter(), new RoleplayNarrator(roleplayLlm),
            new ChatSummarizer(new StubModelCallPort(32_768, "FOLDED SUMMARY", 0)),
            new NextActReadinessAnalyst(roleplayLlm), new NpcMindStateAnalyst(roleplayLlm),
            new ReplyOriginAnalyst(analysisLlm), LogPort.NOOP, new Random(1));
    }

    @Test
    void theOriginOfTheLastReplyIsMappedFromThePromptActuallySent(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn");
        RecordingModelCallPort analysisLlm = new RecordingModelCallPort(
            "TRACED:\n- a nod -> YOUR CHARACTER : \"grumpy\" [indirect]\nUNTRACED:\n[RIEN]\nREADING:\nThe sheet weighs most.");
        ChatServiceImpl service = newServiceWithOriginAnalyst(analysisLlm);
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, true);

        service.sendMessage(root, session, "Hello there.");
        ReplyOriginAnalystOutput out = service.analyzeLastReplyOrigin(root, session);

        assertTrue(out.traced().contains("YOUR CHARACTER"));
        assertTrue(analysisLlm.lastUserPrompt.contains("A grumpy innkeeper."), "le prompt reellement envoye au personnage");
        assertTrue(analysisLlm.lastUserPrompt.contains("===== REPLY =====\ngrunts and nods slowly"));
        assertTrue(analysisLlm.lastUserPrompt.contains("Hello there."));
        assertEquals(2, session.turns().size(), "lecture seule : aucun tour ajoute");
    }

    @Test
    void withoutAnyReplyOrOnceItIsUndoneThereIsNothingToLookAt(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn");
        ChatServiceImpl service = newServiceWithOriginAnalyst(
            new RecordingModelCallPort("TRACED:\n[RIEN]\nUNTRACED:\n[RIEN]\nREADING:\n-"));
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, true);

        assertThrows(IllegalStateException.class, () -> service.analyzeLastReplyOrigin(root, session));

        service.sendMessage(root, session, "Hello there.");
        service.analyzeLastReplyOrigin(root, session); // ok : une reponse existe
        service.undo(root, session, 1);

        assertThrows(IllegalStateException.class, () -> service.analyzeLastReplyOrigin(root, session));
    }

    @Test
    void withoutTheAnalystWiredTheButtonSaysSoInsteadOfFailingObscurely(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn");
        ChatServiceImpl service = newService(32_768);
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, true);
        service.sendMessage(root, session, "Hello there.");

        assertThrows(IllegalStateException.class, () -> service.analyzeLastReplyOrigin(root, session));
    }

    @Test
    void sendMessageAppendsBothTurnsAndPersists(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn");
        ChatServiceImpl service = newService(32_768); // grand contexte -> jamais de compaction ici
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, true);

        ChatTurnResult result = service.sendMessage(root, session, "Hello there.");

        assertEquals(ChatTurn.Speaker.PLAYER, result.playerTurn().speaker());
        assertEquals("Hello there.", result.playerTurn().text());
        assertEquals(ChatTurn.Speaker.LLM, result.replyTurns().get(0).speaker());
        assertEquals("grunts and nods slowly", result.replyTurns().get(0).text());
        assertFalse(result.compacted());
        assertEquals(2, session.turns().size());

        // persiste : un nouveau service relisant le meme repertoire retrouve les 2 tours
        ChatSession reloaded = newService(32_768).openSession(root, scenario, false);
        assertEquals(2, reloaded.turns().size());
    }

    @Test
    void sendMessageWithListenerNotifiesEveryTurnInOrderAsSoonAsItIsAppended(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn");
        ChatServiceImpl service = newService(32_768);
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, true);

        List<ChatTurn> streamedTurns = new java.util.ArrayList<>();
        ChatTurnResult result = service.sendMessage(root, session, "Hello there.", new ExchangeProgressListener() {
            @Override public void onPartialReply(String npcId, PartialGeneration partial) {}
            @Override public void onTurnReady(ChatTurn turn) { streamedTurns.add(turn); }
        });

        // meme contenu que sendMessage() sans listener, juste notifie au fil de l'eau : le tour
        // joueur d'abord, puis chaque replique — jamais tout d'un coup a la fin.
        assertEquals(List.of(result.playerTurn(), result.replyTurns().get(0)), streamedTurns);
    }

    @Test
    void sendMessageWithListenerNotifiesPartialReplyTextBeforeTheTurnIsReady(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn");
        StreamingStubModelCallPort roleplayLlm = new StreamingStubModelCallPort(32_768,
            List.of("grunts", "grunts and nods"), "grunts and nods slowly");
        StubModelCallPort summarizerLlm = new StubModelCallPort(32_768, "FOLDED SUMMARY", 0);
        ChatServiceImpl service = new ChatServiceImpl(new ChatFileStorageAdapter(), new RoleplayNarrator(roleplayLlm),
            new ChatSummarizer(summarizerLlm), new NextActReadinessAnalyst(roleplayLlm), new NpcMindStateAnalyst(roleplayLlm));
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, true);

        // liste unique pour verifier l'ORDRE relatif des evenements, pas juste leur contenu.
        List<String> events = new java.util.ArrayList<>();
        service.sendMessage(root, session, "Hello there.", new ExchangeProgressListener() {
            @Override public void onPartialReply(String npcId, PartialGeneration partial) {
                events.add("partial:" + partial.textSoFar());
            }
            @Override public void onTurnReady(ChatTurn turn) { events.add("ready:" + turn.speaker()); }
        });

        assertEquals(List.of("ready:PLAYER", "partial:grunts", "partial:grunts and nods", "ready:LLM"), events);
    }

    @Test
    void sendMessageRemovesTheOrphanedPlayerTurnWhenGenerationIsCancelled(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn");
        CancellingModelCallPort roleplayLlm = new CancellingModelCallPort();
        StubModelCallPort summarizerLlm = new StubModelCallPort(32_768, "FOLDED SUMMARY", 0);
        ChatServiceImpl service = new ChatServiceImpl(
            new ChatFileStorageAdapter(), new RoleplayNarrator(roleplayLlm), new ChatSummarizer(summarizerLlm),
            new NextActReadinessAnalyst(roleplayLlm), new NpcMindStateAnalyst(roleplayLlm));
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, true);

        assertThrows(GenerationCancelledException.class, () -> service.sendMessage(root, session, "Hello there."));

        assertTrue(session.turns().isEmpty());
    }

    @Test
    void cancellingAfterTheFirstNpcAlreadyRepliedKeepsThatReplyInsteadOfDiscardingTheRound(@TempDir Path root)
            throws IOException {
        // elena et marcus sont tous les deux nommes explicitement (round a 2 principaux, pas
        // d'alea d'interjection a gerer) : elena repond, puis le stub annule l'appel de marcus —
        // sa reponse a elle ne doit plus etre effacee juste parce que le round n'a pas fini.
        writeTwoNpcScenarioFiles(root, "inn");
        SucceedThenCancelModelCallPort roleplayLlm = new SucceedThenCancelModelCallPort(1, "grunts and nods slowly");
        ChatServiceImpl service = new ChatServiceImpl(new ChatFileStorageAdapter(), new RoleplayNarrator(roleplayLlm),
            new ChatSummarizer(new StubModelCallPort(32_768, "FOLDED", 0)), new NextActReadinessAnalyst(roleplayLlm),
            new NpcMindStateAnalyst(roleplayLlm));
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, true);

        ChatTurnResult result = service.sendMessage(root, session, "Elena and Marcus, look at this!");

        assertEquals(1, result.replyTurns().size(), "marcus annule en cours de round, seule la reponse d'elena survit");
        assertEquals("elena", result.replyTurns().get(0).npcId());
        assertEquals(2, session.turns().size(), "tour joueur + reponse d'elena, pas d'annulation du round entier");

        // persiste : la reponse gardee n'est pas juste restee en memoire
        ChatSession reloaded = newService(32_768).openSession(root, scenario, false);
        assertEquals(2, reloaded.turns().size());
    }

    @Test
    void cancellingDuringBackgroundCompactionKeepsTheRoundAndSkipsCompactionInstead(@TempDir Path root)
            throws IOException {
        writeScenarioFiles(root, "inn");
        // fenetre minuscule -> budget dynamique clampe a 0, la compaction se declenche des le 3e tour
        StubModelCallPort roleplayLlm = new StubModelCallPort(80, "grunts and nods slowly", 0);
        CancellingModelCallPort summarizerLlm = new CancellingModelCallPort();
        ChatServiceImpl service = new ChatServiceImpl(new ChatFileStorageAdapter(), new RoleplayNarrator(roleplayLlm),
            new ChatSummarizer(summarizerLlm), new NextActReadinessAnalyst(roleplayLlm), new NpcMindStateAnalyst(roleplayLlm));
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, true);

        service.sendMessage(root, session, "one two three four five");
        service.sendMessage(root, session, "six seven eight nine ten");
        ChatTurnResult third = service.sendMessage(root, session, "eleven twelve thirteen");

        assertFalse(third.compacted(), "la compaction a ete annulee, traitee comme un resume vide (voir compactIfNeeded)");
        assertEquals(6, session.turns().size(), "aucun tour perdu : la reponse du 3e tour survit malgre l'annulation de la compaction");
    }

    @Test
    void doPrefixIsKeptInStoredTurnText(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn");
        ChatServiceImpl service = newService(32_768);
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, true);

        ChatTurnResult result = service.sendMessage(root, session, "DO: the storm knocks out the lights.");

        assertEquals("DO: the storm knocks out the lights.", result.playerTurn().text());
    }

    @Test
    void retryReplacesTheLastReplyWithoutDuplicatingThePlayerTurn(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn");
        ChatServiceImpl service = newService(32_768);
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, true);
        service.sendMessage(root, session, "Hello there.");
        assertEquals(2, session.turns().size());

        ChatTurnResult result = service.retry(root, session);

        assertEquals(1, result.replacedTurnCount());
        assertEquals(2, session.turns().size(), "toujours 2 tours : le tour joueur inchange + la nouvelle reponse");
        assertEquals("Hello there.", session.turns().get(0).text());
        assertEquals(result.replyTurns().get(0).text(), session.turns().get(1).text());
    }

    @Test
    void retryFailsWhenThereIsNoReplyToRegenerate(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn");
        ChatServiceImpl service = newService(32_768);
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, true);

        assertThrows(IllegalStateException.class, () -> service.retry(root, session));
    }

    @Test
    void undoRemovesTheLastPlayerAndReplyTurnsAndPersists(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn");
        ChatServiceImpl service = newService(32_768);
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, true);
        service.sendMessage(root, session, "Hello there.");
        assertEquals(2, session.turns().size());

        int removed = service.undo(root, session, 1);

        assertEquals(2, removed);
        assertTrue(session.turns().isEmpty());
        ChatSession reloaded = newService(32_768).openSession(root, scenario, false);
        assertTrue(reloaded.turns().isEmpty());
    }

    @Test
    void undoIsANoOpWhenThereIsNoPlayerTurnToRemove(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn");
        ChatServiceImpl service = newService(32_768);
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, true);

        assertEquals(0, service.undo(root, session, 1));
    }

    @Test
    void undoWithMultipleStepsRemovesThatManyExchanges(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn");
        ChatServiceImpl service = newService(32_768);
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, true);
        service.sendMessage(root, session, "First.");
        service.sendMessage(root, session, "Second.");
        service.sendMessage(root, session, "Third.");
        assertEquals(6, session.turns().size());

        int removed = service.undo(root, session, 2);

        assertEquals(4, removed);
        assertEquals(2, session.turns().size());
        assertEquals("First.", session.turns().get(0).text());
    }

    @Test
    void undoClampsToWhatIsAvailableWhenStepsExceedsPlayerTurns(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn");
        ChatServiceImpl service = newService(32_768);
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, true);
        service.sendMessage(root, session, "Only one.");
        assertEquals(2, session.turns().size());

        int removed = service.undo(root, session, 5);

        assertEquals(2, removed);
        assertTrue(session.turns().isEmpty());
    }

    @Test
    void noCompactionWhileAtOrBelowMinKeepTurnsEvenIfContextIsTiny(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn");
        ChatServiceImpl service = newService(80); // fenetre minuscule -> budget dynamique clampe a 0, depasse au moindre tour
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, true);

        // 1 echange = 2 tours = MIN_KEEP_TURNS : jamais compacte, quelle que soit la taille du texte
        ChatTurnResult result = service.sendMessage(root, session, "one two three four five");

        assertFalse(result.compacted());
        assertEquals(2, session.turns().size());
        assertEquals("", session.summary());
    }

    @Test
    void compactsOnceTurnCountAndTokenBudgetAreBothExceeded(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn");
        ChatServiceImpl service = newService(80); // fenetre minuscule -> budget dynamique clampe a 0, depasse au moindre tour
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, true);

        service.sendMessage(root, session, "one two three four five");
        service.sendMessage(root, session, "six seven eight nine ten");
        ChatTurnResult third = service.sendMessage(root, session, "eleven twelve thirteen");

        assertTrue(third.compacted());
        assertEquals(2, session.turns().size(), "fenetre minuscule -> keepTokensTarget clampe a 0, MIN_KEEP_TURNS s'applique");
        assertEquals("FOLDED SUMMARY", session.summary());

        // les tours replies ne sont pas perdus : ils restent verbatim dans leur fichier, et la
        // compaction est un fichier de la meme suite (apres le tour 6, repliant jusqu'au tour 4)
        String firstTurn = Files.readString(root.resolve("inn/run/current/turns/000001.md"), StandardCharsets.UTF_8);
        assertTrue(firstTurn.contains("one two three four five"), "premier tour replie, doit rester sur disque");
        assertTrue(turnFileNames(root, "inn").contains("000006-compaction-f000004.md"));
        assertEquals("FOLDED SUMMARY",
            Files.readString(root.resolve("inn/run/current/turns/000006-compaction-f000004.md"), StandardCharsets.UTF_8));
    }

    @Test
    void compactionIsSkippedAndHistoryPreservedWhenSummarizerReturnsBlank(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn");
        StubModelCallPort roleplayLlm = new StubModelCallPort(80, "grunts and nods slowly", 0);
        StubModelCallPort blankSummarizerLlm = new StubModelCallPort(32_768, "   ", 0); // resume vide/blanc
        ChatServiceImpl service = new ChatServiceImpl(new ChatFileStorageAdapter(), new RoleplayNarrator(roleplayLlm),
            new ChatSummarizer(blankSummarizerLlm), new NextActReadinessAnalyst(roleplayLlm), new NpcMindStateAnalyst(roleplayLlm));
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, true);

        service.sendMessage(root, session, "one two three four five");
        service.sendMessage(root, session, "six seven eight nine ten");
        ChatTurnResult third = service.sendMessage(root, session, "eleven twelve thirteen");

        assertFalse(third.compacted(), "un resume vide ne doit jamais declencher un compactage");
        assertEquals(6, session.turns().size(), "aucun tour perdu : l'historique complet reste dans la session");
        assertEquals("", session.summary());
        assertTrue(turnFileNames(root, "inn").stream().noneMatch(n -> n.contains("compaction")),
            "rien n'a ete replie, donc aucun fichier de compaction");
    }

    @Test
    void contextWindowDelegatesToTheRoleplayPort(@TempDir Path root) {
        ChatServiceImpl service = newService(4_096);
        assertEquals(4_096, service.contextWindow());
    }

    @Test
    void turnResultCarriesAnEstimatedTotalForTheSessionsCurrentState(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn");
        // le stub renvoie promptTokens=1_234 (mesure Ollama de l'appel qui vient de se terminer) : la
        // jauge ne doit plus s'y fier, elle doit refleter une estimation de l'etat courant (apres ce
        // tour), donc une tout autre valeur.
        ChatServiceImpl service = newService(32_768, 1_234);
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, true);

        ChatTurnResult result = service.sendMessage(root, session, "Hello there.");

        Scene scene = new Scene(scenario.cast().npcs().get(0), List.of(), List.of(), false);
        ChatPromptBuilder.ChatPrompt next = ChatPromptBuilder.build(scenario, scene, session.currentAct(),
            session.summary(), session.turns());
        int expected = ChatContextBudget.estimateTokens(next.system() + next.user());
        assertEquals(expected, result.promptTokens());
        assertTrue(result.promptTokens() > 0);
    }

    // ── Partie a reprendre ? ────────────────────────────────────────────────

    @Test
    void hasSavedSessionIsFalseUntilTheFirstExchangeAndAgainAfterARestart(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn");
        writeActs(root, "inn", "[The door creaks open.]\n\nFirst act.");
        ChatServiceImpl service = newService(32_768);
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, false);
        assertFalse(service.hasSavedSession(root, scenario), "jamais joue : rien a reprendre");
        assertEquals(1, session.turns().size(), "seule l'introduction du narrateur");

        service.sendMessage(root, session, "Hello there.");
        assertTrue(service.hasSavedSession(root, scenario));

        service.restartSession(root, session);
        assertFalse(service.hasSavedSession(root, scenario), "apres Recommencer il ne reste que l'introduction");
    }

    // ── Retour en arriere (rollbackTo) ──────────────────────────────────────

    @Test
    void rollbackToRestoresTheStateJustAfterThatTurnDeletesWhatFollowedAndReusesItsNumbers(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn");
        ChatServiceImpl service = newService(32_768);
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, true);
        service.sendMessage(root, session, "First.");
        service.sendMessage(root, session, "Second.");
        service.sendMessage(root, session, "Third.");
        assertEquals(6, session.turns().size());

        int removed = service.rollbackTo(root, session, TurnNumber.of(2));

        assertEquals(4, removed);
        assertEquals(2, session.turns().size());
        assertEquals(List.of("000001.md", "000002.md"), turnFileNames(root, "inn"), "destructif : les autres tours sont supprimes du disque");

        service.sendMessage(root, session, "A different second.");

        assertEquals(TurnNumber.of(4), session.lastTurnNumber(), "le tour suivant reprend le numero N+1");
        assertEquals(List.of("000001.md", "000002.md", "000003.md", "000004.md"), turnFileNames(root, "inn"));
        assertEquals("A different second.", session.turns().get(2).text());
    }

    @Test
    void rollbackToTheLastTurnIsANoOp(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn");
        ChatServiceImpl service = newService(32_768);
        ChatSession session = service.openSession(root, service.loadScenario(root, "inn"), true);
        service.sendMessage(root, session, "Hello.");

        assertEquals(0, service.rollbackTo(root, session, TurnNumber.of(2)));
        assertEquals(2, session.turns().size());
    }

    @Test
    void rollbackToATurnBeyondTheLastOneIsRejected(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn");
        ChatServiceImpl service = newService(32_768);
        ChatSession session = service.openSession(root, service.loadScenario(root, "inn"), true);
        service.sendMessage(root, session, "Hello.");

        assertThrows(IllegalArgumentException.class, () -> service.rollbackTo(root, session, TurnNumber.of(3)));
        assertEquals(2, session.turns().size());
    }

    @Test
    void rollbackToBeforeACompactionBringsBackTheOlderSummaryStateAndDeletesTheLaterCompactions(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn");
        ChatServiceImpl tiny = newService(80); // fenetre minuscule : compacte des le 2e echange
        ChatScenario scenario = tiny.loadScenario(root, "inn");
        ChatSession built = tiny.openSession(root, scenario, true);
        tiny.sendMessage(root, built, "one two three four five");
        tiny.sendMessage(root, built, "six seven eight nine ten");
        tiny.sendMessage(root, built, "eleven twelve thirteen");
        // compactions apres les tours 4 (repliant jusqu'au 2) puis 6 (repliant jusqu'au 4)
        assertTrue(turnFileNames(root, "inn").containsAll(List.of("000004-compaction-f000002.md", "000006-compaction-f000004.md")));

        // une grande fenetre pour la suite : aucune nouvelle compaction ne doit etre declenchee par le retour
        ChatServiceImpl roomy = newService(32_768);
        ChatSession session = roomy.openSession(root, scenario, false);
        assertEquals(TurnNumber.of(5), session.firstTurnNumber());

        roomy.rollbackTo(root, session, TurnNumber.of(4));
        assertEquals("FOLDED SUMMARY", session.summary(), "la compaction faite apres le tour 4 s'applique deja au tour 4");
        assertEquals(TurnNumber.of(3), session.firstTurnNumber());
        assertEquals(2, session.turns().size());
        assertFalse(turnFileNames(root, "inn").contains("000006-compaction-f000004.md"));

        roomy.rollbackTo(root, session, TurnNumber.of(3));
        assertEquals("", session.summary(), "avant la premiere compaction : pas de resume");
        assertEquals(TurnNumber.of(1), session.firstTurnNumber());
        assertEquals(3, session.turns().size());
        assertTrue(turnFileNames(root, "inn").stream().noneMatch(n -> n.contains("compaction")));
    }

    @Test
    void rollbackToRestoresTheActThatTurnWasWrittenIn(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn");
        writeActs(root, "inn", "First act.", "Second act.");
        StubModelCallPort roleplayLlm = new StubModelCallPort(32_768, "*nods* Time to move on. [NEXT ACT]", 0);
        ChatServiceImpl service = new ChatServiceImpl(new ChatFileStorageAdapter(), new RoleplayNarrator(roleplayLlm),
            new ChatSummarizer(new StubModelCallPort(32_768, "FOLDED", 0)), new NextActReadinessAnalyst(roleplayLlm), new NpcMindStateAnalyst(roleplayLlm));
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, true);
        service.sendMessage(root, session, "Let's go.");
        assertEquals(2, session.currentAct(), "la reponse a declenche [NEXT ACT]");

        service.rollbackTo(root, session, TurnNumber.of(1));

        assertEquals(1, session.currentAct());
        ChatSession reloaded = newService(32_768).openSession(root, scenario, false);
        assertEquals(1, reloaded.currentAct(), "persiste aussi : state.txt reflete l'acte restaure");
    }

    @Test
    void rollbackToNeverRestoresPresenceBecauseThoseAreThePlayersOwnSettings(@TempDir Path root) throws IOException {
        writeTwoNpcScenarioFiles(root, "inn");
        ChatServiceImpl service = newService(32_768);
        ChatSession session = service.openSession(root, service.loadScenario(root, "inn"), true);
        service.sendMessage(root, session, "Hello Elena.");
        mute(service, root, session, "marcus");

        service.rollbackTo(root, session, TurnNumber.of(1));

        assertEquals(Set.of("elena"), session.presentNpcIds());
        ChatSession reloaded = newService(32_768).openSession(root, service.loadScenario(root, "inn"), false);
        assertEquals(Set.of("elena"), reloaded.presentNpcIds());
    }

    @Test
    void aSavePointKeepsTheWholeHistorySoARollbackStillWorksAfterLoadingIt(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn");
        ChatServiceImpl service = newService(32_768);
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, true);
        service.sendMessage(root, session, "First.");
        service.sendMessage(root, session, "Second.");
        SavePoint save = service.save(root, session);
        service.sendMessage(root, session, "Third, after the save.");

        service.loadSavePoint(root, session, save.id());
        assertEquals(4, session.turns().size());
        service.rollbackTo(root, session, TurnNumber.of(2));

        assertEquals(2, session.turns().size());
        assertEquals("First.", session.turns().get(0).text());
    }

    @Test
    void undoAfterAnActAdvanceBringsTheActBackToo(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn");
        writeActs(root, "inn", "First act.", "Second act.");
        StubModelCallPort roleplayLlm = new StubModelCallPort(32_768, "*nods* [NEXT ACT]", 0);
        ChatServiceImpl service = new ChatServiceImpl(new ChatFileStorageAdapter(), new RoleplayNarrator(roleplayLlm),
            new ChatSummarizer(new StubModelCallPort(32_768, "FOLDED", 0)), new NextActReadinessAnalyst(roleplayLlm), new NpcMindStateAnalyst(roleplayLlm));
        ChatSession session = service.openSession(root, service.loadScenario(root, "inn"), true);
        service.sendMessage(root, session, "Let's go.");
        assertEquals(2, session.currentAct());

        service.undo(root, session, 1);

        assertEquals(1, session.currentAct(), "undo est un retour en arriere : l'acte revient aussi");
        assertTrue(session.turns().isEmpty());
    }

    @Test
    void everyTurnIsOnDiskAsSoonAsItExistsNotOnlyAtTheEndOfTheRound(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn");
        ChatServiceImpl service = newService(32_768);
        ChatSession session = service.openSession(root, service.loadScenario(root, "inn"), true);
        List<List<String>> filesSeenAtEachTurn = new java.util.ArrayList<>();

        service.sendMessage(root, session, "Hello.", new ExchangeProgressListener() {
            @Override public void onPartialReply(String npcId, PartialGeneration partial) { }
            @Override public void onTurnReady(ChatTurn turn) {
                try { filesSeenAtEachTurn.add(turnFileNames(root, "inn")); } catch (IOException e) { throw new RuntimeException(e); }
            }
        });

        assertEquals(List.of(List.of("000001.md"), List.of("000001.md", "000002.md")), filesSeenAtEachTurn);
    }

    // ── Progressive acts ────────────────────────────────────────────────────

    /** Appends flat, top-level "# " act headings to the scenario's already-written scenario.txt. */
    private static void writeActs(Path root, String scenarioName, String... actBodies) throws IOException {
        Path file = root.resolve(scenarioName).resolve("scenario.txt");
        StringBuilder sb = new StringBuilder(Files.readString(file, StandardCharsets.UTF_8));
        for (String body : actBodies) {
            sb.append("\n\n# \n").append(body);
        }
        Files.writeString(file, sb.toString(), StandardCharsets.UTF_8);
    }

    @Test
    void nextActMarkerInReplyAdvancesTheActAndIsStrippedFromStoredText(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn");
        writeActs(root, "inn", "First act.", "Second act.");
        StubModelCallPort roleplayLlm = new StubModelCallPort(32_768, "*nods* Time to move on. [NEXT ACT]", 0);
        ChatServiceImpl service = new ChatServiceImpl(new ChatFileStorageAdapter(), new RoleplayNarrator(roleplayLlm),
            new ChatSummarizer(new StubModelCallPort(32_768, "FOLDED", 0)), new NextActReadinessAnalyst(roleplayLlm), new NpcMindStateAnalyst(roleplayLlm));
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, true);
        assertEquals(1, session.currentAct());

        ChatTurnResult result = service.sendMessage(root, session, "Let's go.");

        assertTrue(result.actAdvanced());
        assertEquals(2, session.currentAct());
        assertFalse(result.replyTurns().get(0).text().contains("[NEXT ACT]"));
        assertEquals("*nods* Time to move on.", result.replyTurns().get(0).text());
    }

    @Test
    void newTurnsIncludesTheNarratorBeatsAddedByAnActAdvanceTriggeredThisTurn(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn");
        writeActs(root, "inn", "First act.", "Second act.\n[They found the map.]\n\nBody text.");
        StubModelCallPort roleplayLlm = new StubModelCallPort(32_768, "*nods* [NEXT ACT]", 0);
        ChatServiceImpl service = new ChatServiceImpl(new ChatFileStorageAdapter(), new RoleplayNarrator(roleplayLlm),
            new ChatSummarizer(new StubModelCallPort(32_768, "FOLDED", 0)), new NextActReadinessAnalyst(roleplayLlm), new NpcMindStateAnalyst(roleplayLlm));
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, true);

        ChatTurnResult result = service.sendMessage(root, session, "Let's go.");

        assertTrue(result.actAdvanced());
        assertEquals(List.of(result.playerTurn(), result.replyTurns().get(0),
            new ChatTurn(ChatTurn.Speaker.NARRATOR, "They found the map.")), result.newTurns());
    }

    @Test
    void nextActMarkerOnTheLastActDoesNotAdvanceFurther(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn");
        writeActs(root, "inn", "Only act.");
        StubModelCallPort roleplayLlm = new StubModelCallPort(32_768, "The end. [NEXT ACT]", 0);
        ChatServiceImpl service = new ChatServiceImpl(new ChatFileStorageAdapter(), new RoleplayNarrator(roleplayLlm),
            new ChatSummarizer(new StubModelCallPort(32_768, "FOLDED", 0)), new NextActReadinessAnalyst(roleplayLlm), new NpcMindStateAnalyst(roleplayLlm));
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, true);

        ChatTurnResult result = service.sendMessage(root, session, "Let's go.");

        assertFalse(result.actAdvanced());
        assertEquals(1, session.currentAct());
    }

    @Test
    void replyWithoutMarkerNeverAdvancesTheAct(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn");
        writeActs(root, "inn", "First act.", "Second act.");
        ChatServiceImpl service = newService(32_768);
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, true);

        ChatTurnResult result = service.sendMessage(root, session, "Let's go.");

        assertFalse(result.actAdvanced());
        assertEquals(1, session.currentAct());
    }

    @Test
    void manualAdvanceActMovesToNextActAndPersists(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn");
        writeActs(root, "inn", "First act.", "Second act.");
        ChatServiceImpl service = newService(32_768);
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, true);

        boolean advanced = service.advanceAct(root, session);

        assertTrue(advanced);
        assertEquals(2, session.currentAct());
        ChatSession reloaded = newService(32_768).openSession(root, scenario, false);
        assertEquals(2, reloaded.currentAct());
    }

    @Test
    void advancingToAnActWithStoryBeatsAppendsThemAsNarratorTurns(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn");
        writeActs(root, "inn", "First act.", "Second act.\n[They found the map.]\n\nBody text.");
        ChatServiceImpl service = newService(32_768);
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, true);

        service.advanceAct(root, session);

        assertEquals(ChatTurn.Speaker.NARRATOR, session.turns().get(session.turns().size() - 1).speaker());
        assertEquals("They found the map.", session.turns().get(session.turns().size() - 1).text());

        // persiste : un nouveau service relisant le meme repertoire retrouve le tour NARRATOR
        ChatSession reloaded = newService(32_768).openSession(root, scenario, false);
        assertTrue(reloaded.turns().stream()
            .anyMatch(t -> t.speaker() == ChatTurn.Speaker.NARRATOR && t.text().equals("They found the map.")));
    }

    @Test
    void manualAdvanceActIsANoOpPastTheLastAct(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn");
        writeActs(root, "inn", "Only act.");
        ChatServiceImpl service = newService(32_768);
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, true);

        boolean advanced = service.advanceAct(root, session);

        assertFalse(advanced);
        assertEquals(1, session.currentAct());
    }

    @Test
    void manualPreviousActMovesBackAndPersists(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn");
        writeActs(root, "inn", "First act.", "Second act.");
        ChatServiceImpl service = newService(32_768);
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, true);
        service.advanceAct(root, session);

        boolean movedBack = service.previousAct(root, session);

        assertTrue(movedBack);
        assertEquals(1, session.currentAct());
        ChatSession reloaded = newService(32_768).openSession(root, scenario, false);
        assertEquals(1, reloaded.currentAct());
    }

    @Test
    void manualPreviousActIsANoOpOnTheFirstAct(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn");
        writeActs(root, "inn", "First act.", "Second act.");
        ChatServiceImpl service = newService(32_768);
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, true);

        boolean movedBack = service.previousAct(root, session);

        assertFalse(movedBack);
        assertEquals(1, session.currentAct());
    }

    @Test
    void analyzeNextActReadinessReturnsTheAnalystsParsedOutputWithoutTouchingTheSession(@TempDir Path root)
            throws IOException {
        writeScenarioFiles(root, "inn");
        writeActs(root, "inn", "First act.", "Second act.");
        StubModelCallPort roleplayLlm = new StubModelCallPort(32_768, """
            CONDITION:
            They must leave the inn.
            STATE:
            Not met yet.
            MISSING:
            They have not moved.""", 0);
        ChatServiceImpl service = new ChatServiceImpl(new ChatFileStorageAdapter(), new RoleplayNarrator(roleplayLlm),
            new ChatSummarizer(new StubModelCallPort(32_768, "FOLDED", 0)), new NextActReadinessAnalyst(roleplayLlm), new NpcMindStateAnalyst(roleplayLlm));
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, true);
        List<ChatTurn> turnsBefore = session.turns();

        NextActReadinessAnalystOutput result = service.analyzeNextActReadiness(root, session);

        assertEquals("They must leave the inn.", result.conditionUnderstood());
        assertEquals("Not met yet.", result.currentState());
        assertEquals("They have not moved.", result.missing());
        assertEquals(turnsBefore, session.turns(), "lecture seule : aucun tour ajoute");
    }

    @Test
    void analyzeNextActReadinessFailsWhenThereIsNoNextActToCheck(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn");
        writeActs(root, "inn", "Only act.");
        ChatServiceImpl service = newService(32_768);
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, true);

        assertThrows(IllegalStateException.class, () -> service.analyzeNextActReadiness(root, session));
    }

    @Test
    void analyzeMindStateReturnsTheAnalystsParsedOutputWithoutTouchingTheSession(@TempDir Path root)
            throws IOException {
        writeScenarioFiles(root, "inn");
        StubModelCallPort roleplayLlm = new StubModelCallPort(32_768, """
            SITUATION:
            She is behind the bar, alone with the player.
            THOUGHTS:
            She does not trust him yet.
            PLANS:
            Keep the conversation short.""", 0);
        ChatServiceImpl service = new ChatServiceImpl(new ChatFileStorageAdapter(), new RoleplayNarrator(roleplayLlm),
            new ChatSummarizer(new StubModelCallPort(32_768, "FOLDED", 0)), new NextActReadinessAnalyst(roleplayLlm),
            new NpcMindStateAnalyst(roleplayLlm));
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, true);
        List<ChatTurn> turnsBefore = session.turns();

        NpcMindStateAnalystOutput result = service.analyzeMindState(root, session, "character");

        assertEquals("She is behind the bar, alone with the player.", result.situation());
        assertEquals("She does not trust him yet.", result.thoughts());
        assertEquals("Keep the conversation short.", result.plans());
        assertEquals(turnsBefore, session.turns(), "lecture seule : aucun tour ajoute");
    }

    @Test
    void analyzeMindStateFailsForAnUnknownOrAbsentNpc(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn");
        ChatServiceImpl service = newService(32_768);
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, true);

        assertThrows(IllegalStateException.class, () -> service.analyzeMindState(root, session, "ghost"));
    }

    @Test
    void saveCreatesASavePointFromTheCurrentSessionState(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn");
        ChatServiceImpl service = newService(32_768);
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, true);
        service.sendMessage(root, session, "Hello there.");

        SavePoint save = service.save(root, session);

        assertEquals(List.of(save), service.listSavePoints(root, scenario));
    }

    @Test
    void loadSavePointOverwritesTheSessionWithoutCreatingAnAutomaticSave(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn");
        ChatServiceImpl service = newService(32_768);
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, true);
        service.sendMessage(root, session, "First message.");
        SavePoint earlySave = service.save(root, session);
        service.sendMessage(root, session, "Second message, about to be lost.");
        assertEquals(4, session.turns().size());

        service.loadSavePoint(root, session, earlySave.id());

        assertEquals(2, session.turns().size(), "l'etat restaure ne contient que le 1er echange");
        assertEquals("First message.", session.turns().get(0).text());
        // pas de sauvegarde de securite : toujours un seul point (celui cree explicitement plus haut)
        assertEquals(List.of(earlySave), service.listSavePoints(root, scenario));

        // persiste aussi sur le slot de session live, pas seulement en memoire
        ChatSession reloaded = newService(32_768).openSession(root, scenario, false);
        assertEquals(2, reloaded.turns().size());
    }

    @Test
    void restartSessionWipesTheConversationAndResetsPresence(@TempDir Path root) throws IOException {
        writeTwoNpcScenarioFiles(root, "inn");
        ChatServiceImpl service = newService(32_768);
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, true);
        service.sendMessage(root, session, "Hello there.");
        mute(service, root, session, "marcus");
        assertEquals(Set.of("elena"), session.presentNpcIds());

        service.restartSession(root, session);

        assertEquals(List.of(), session.turns(), "retour a une conversation vierge");
        assertEquals(Set.of("elena", "marcus"), session.presentNpcIds(), "la presence repart de zero, pas de mute qui survit");
        // Cast n'a pas d'equals() structurel, donc pas de comparaison exacte scenario/session.scenario()
        // ici : la relecture depuis le disque produit une instance differente meme a contenu identique
        // (voir restartSessionPicksUpEditsMadeToTheScenarioFilesSinceTheSessionWasOpened, plus bas).
        assertEquals(scenario.name(), session.scenario().name());

        // persiste aussi sur le slot de session live, pas seulement en memoire
        ChatSession reloaded = newService(32_768).openSession(root, scenario, false);
        assertEquals(List.of(), reloaded.turns());
        assertEquals(Set.of("elena", "marcus"), reloaded.presentNpcIds());
    }

    @Test
    void restartSessionPicksUpEditsMadeToTheScenarioFilesSinceTheSessionWasOpened(@TempDir Path root) throws IOException {
        // "Recommencer" doit repartir du contenu ACTUEL sur le disque, pas rejouer le scenario
        // encore en memoire depuis l'ouverture de la session — sinon une correction de scenario.txt
        // ou d'une fiche perso pendant la partie est ignoree tant qu'on ne fait pas explicitement
        // Recharger avant de Recommencer.
        writeTwoNpcScenarioFiles(root, "inn");
        ChatServiceImpl service = newService(32_768);
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, true);
        service.sendMessage(root, session, "Hello there.");

        Files.writeString(root.resolve("inn").resolve("characters").resolve("elena.txt"), "# Elena\nUpdated public sheet.",
            StandardCharsets.UTF_8);

        service.restartSession(root, session);

        assertTrue(session.scenario().cast().find("elena").orElseThrow().publicInfo().contains("Updated public sheet."),
            "la fiche relue depuis le disque doit refleter l'edition, pas l'ancienne version en memoire");
    }

    @Test
    void restartSessionWithAnInvalidScenarioChangesNothingAndListsTheErrors(@TempDir Path root) throws IOException {
        // Le scenario est valide a l'ouverture, puis l'auteur l'abime (commande inconnue) : "Recommencer"
        // doit refuser AVANT d'archiver ou de supprimer quoi que ce soit, sinon la partie en cours
        // disparait du disque alors que la session en memoire, elle, n'a pas bouge.
        writeTwoNpcScenarioFiles(root, "inn");
        ChatServiceImpl service = newService(32_768);
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, true);
        service.sendMessage(root, session, "Hello there.");
        int turnsBefore = session.turns().size();
        Path scenarioFile = root.resolve("inn").resolve("scenario.txt");
        Files.writeString(scenarioFile, Files.readString(scenarioFile, StandardCharsets.UTF_8) + "\n{teleport elena}\n", StandardCharsets.UTF_8);

        ScenarioValidationException e = assertThrows(ScenarioValidationException.class, () -> service.restartSession(root, session));

        assertTrue(e.getMessage().contains("teleport"), e.getMessage());
        assertEquals(turnsBefore, session.turns().size(), "la conversation en memoire n'a pas bouge");
        assertTrue(Files.isDirectory(root.resolve("inn/run/current")), "la partie en cours est toujours sur le disque");
        assertFalse(Files.exists(root.resolve("inn/run/archive")), "rien n'a ete archive");
        assertEquals(turnsBefore, newService(32_768).openSession(root, scenario, false).turns().size(),
            "la partie se rouvre telle qu'elle etait");
    }

    @Test
    void aRunIsQueriedAndArchivedByNameBeforeTheScenarioIsLoaded(@TempDir Path root) throws IOException {
        // Le CLI pose "Continuer ?" AVANT de charger le scenario (le chargement relit le run et peut y
        // echouer) : il n'a donc que le nom. Repartir de zero archive le run, jamais ne le supprime.
        writeTwoNpcScenarioFiles(root, "inn");
        ChatServiceImpl service = newService(32_768);
        assertFalse(service.hasRun(root, "inn"), "aucun run avant la premiere partie");
        assertFalse(service.hasSavedSession(root, "inn"));
        ChatSession session = service.openSession(root, service.loadScenario(root, "inn"), true);
        service.sendMessage(root, session, "Hello there.");
        assertTrue(service.hasRun(root, "inn"));
        assertTrue(service.hasSavedSession(root, "inn"), "des tours ont ete joues");

        service.archiveRun(root, "inn");

        assertFalse(service.hasRun(root, "inn"));
        assertFalse(service.hasSavedSession(root, "inn"));
        try (var archives = Files.list(root.resolve("inn/run/archive"))) {
            assertEquals(1, archives.count(), "la partie est archivee, pas perdue");
        }
        assertEquals(List.of(), service.openSession(root, service.loadScenario(root, "inn"), false).turns().stream()
            .filter(t -> t.speaker() != ChatTurn.Speaker.NARRATOR).toList(), "le scenario se recharge sur une partie vierge");
    }

    @Test
    void deleteSavePointRemovesItFromTheListing(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn");
        ChatServiceImpl service = newService(32_768);
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, true);
        SavePoint save = service.save(root, session);

        service.deleteSavePoint(root, scenario, save.id());

        assertEquals(List.of(), service.listSavePoints(root, scenario));
    }

    // ── Multi-NPC : SpeakerSelector cable dans le service ───────────────────

    @Test
    void mentioningOneNpcByNameMakesOnlyThatOneReply(@TempDir Path root) throws IOException {
        writeTwoNpcScenarioFiles(root, "inn");
        ChatServiceImpl service = newService(32_768);
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, true);
        // sans ca, marcus (present) tirerait sa chance d'interjection —
        // ce test porte sur SpeakerSelector.select() seul, pas sur rollInterjectors (teste a part).
        session.updateGenerationSettings(neverInterject());

        ChatTurnResult result = service.sendMessage(root, session, "Elena, what do you make of this?");

        assertEquals(1, result.replyTurns().size());
        assertEquals("elena", result.replyTurns().get(0).npcId());
    }

    @Test
    void mutingAnNpcMidRoundStillSkipsTheirAlreadyQueuedTurn(@TempDir Path root) throws IOException {
        // les deux sont nommes explicitement : les deux sont "principaux", pas d'alea d'interjection
        // a gerer dans ce test. On mute marcus juste apres la reponse d'elena (via le listener,
        // qui simule ce qui se passe en vrai si le joueur clique 🔇 pendant qu'un round tourne).
        writeTwoNpcScenarioFiles(root, "inn");
        ChatServiceImpl service = newService(32_768);
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, true);

        ChatTurnResult result = service.sendMessage(root, session, "Elena and Marcus, look!",
            new ExchangeProgressListener() {
                @Override public void onPartialReply(String npcId, PartialGeneration partial) {}
                @Override public void onTurnReady(ChatTurn turn) {
                    if ("elena".equals(turn.npcId())) muteInSession(session, "marcus");
                }
            });

        assertEquals(1, result.replyTurns().size(), "marcus a ete mute apres le tour d'elena, son tour deja programme doit etre saute");
        assertEquals("elena", result.replyTurns().get(0).npcId());
    }

    @Test
    void withNoMentionOnlyOneNpcAnswersByDefault(@TempDir Path root) throws IOException {
        // personne nomme, premier echange (pas de continuation possible) : un seul PNJ est tire au
        // hasard comme principal — le second ne repond que s'il interjecte (teste separement).
        writeTwoNpcScenarioFiles(root, "inn");
        ChatServiceImpl service = newService(32_768);
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, true);
        session.updateGenerationSettings(new GenerationSettings(null, null, null, null, null, null, null, 0.0, null));

        ChatTurnResult result = service.sendMessage(root, session, "Hello there.");

        assertEquals(1, result.replyTurns().size());
    }

    @Test
    void anActiveNpcRepliesAtEveryExchangeEvenWhenSomeoneElseIsNamed(@TempDir Path root) throws IOException {
        writeTwoNpcScenarioFiles(root, "inn");
        Files.writeString(root.resolve("inn/scenario.txt"), "A stormy night.\n\n# Un\nElena and Marcus are at the inn.\n{npc-active Marcus}\n",
            StandardCharsets.UTF_8);
        ChatServiceImpl service = newService(32_768);
        ChatSession session = service.openSession(root, service.loadScenario(root, "inn"), true);
        session.updateGenerationSettings(neverInterject());

        ChatTurnResult first = service.sendMessage(root, session, "Hello there.");
        ChatTurnResult second = service.sendMessage(root, session, "Elena, what do you make of this?");

        assertEquals(List.of("marcus"), first.replyTurns().stream().map(ChatTurn::npcId).toList(),
            "marcus est actif, personne n'est nomme : il parle seul");
        assertEquals(List.of("elena", "marcus"), second.replyTurns().stream().map(ChatTurn::npcId).toList(),
            "elena est nommee, puis l'actif marcus parle aussi");
    }

    @Test
    void anAbsentNpcOfTheScenarioNeverSpeaksAndAClickBringsItBack(@TempDir Path root) throws IOException {
        writeTwoNpcScenarioFiles(root, "inn");
        Files.writeString(root.resolve("inn/scenario.txt"), "A stormy night.\n\n# Un\nElena is at the inn.\n{npc-absent Marcus}\n",
            StandardCharsets.UTF_8);
        ChatServiceImpl service = newService(32_768);
        ChatSession session = service.openSession(root, service.loadScenario(root, "inn"), true);
        session.updateGenerationSettings(alwaysInterject());

        ChatTurnResult before = service.sendMessage(root, session, "Hello there.");
        service.cycleNpcActivity(root, session, "marcus"); // absent (scenario) -> actif (force)
        ChatTurnResult after = service.sendMessage(root, session, "Hello again.");

        assertEquals(List.of("elena"), before.replyTurns().stream().map(ChatTurn::npcId).toList());
        assertTrue(after.replyTurns().stream().anyMatch(t -> "marcus".equals(t.npcId())), "marcus force actif repond");
    }

    @Test
    void withNoMentionBothPresentNpcsCanReplyViaInterjectionAtCertainChance(@TempDir Path root) throws IOException {
        writeTwoNpcScenarioFiles(root, "inn");
        StubModelCallPort roleplayLlm = new StubModelCallPort(32_768, "grunts and nods slowly", 0);
        ChatServiceImpl service = new ChatServiceImpl(new ChatFileStorageAdapter(), new RoleplayNarrator(roleplayLlm),
            new ChatSummarizer(new StubModelCallPort(32_768, "FOLDED", 0)), new NextActReadinessAnalyst(roleplayLlm),
            new NpcMindStateAnalyst(roleplayLlm), LogPort.NOOP, new Random(1));
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, true);
        session.updateGenerationSettings(alwaysInterject());

        ChatTurnResult result = service.sendMessage(root, session, "Hello there.");

        assertEquals(2, result.replyTurns().size(),
            "un seul PNJ principal (tire au hasard), l'autre interjecte a chance certaine");
        assertEquals(Set.of("elena", "marcus"),
            result.replyTurns().stream().map(ChatTurn::npcId).collect(Collectors.toSet()));
    }

    @Test
    void withNoMentionTheNpcWhoAloneSpokeLastRoundContinuesFirst(@TempDir Path root) throws IOException {
        writeTwoNpcScenarioFiles(root, "inn");
        ChatServiceImpl service = newService(32_768);
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, true);
        session.updateGenerationSettings(neverInterject()); // isole select() : pas d'interjection ici
        service.sendMessage(root, session, "Elena, what do you make of this?"); // elena seule a parle

        ChatTurnResult result = service.sendMessage(root, session, "Et après ?"); // personne vise

        assertEquals(1, result.replyTurns().size());
        assertEquals("elena", result.replyTurns().get(0).npcId(), "elena reprend par continuite, seule a avoir parle avant");
    }

    @Test
    void mutingAnNpcExcludesThemFromSpeakerSelection(@TempDir Path root) throws IOException {
        writeTwoNpcScenarioFiles(root, "inn");
        ChatServiceImpl service = newService(32_768);
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, true);
        mute(service, root, session, "marcus");

        ChatTurnResult result = service.sendMessage(root, session, "Hello there.");

        assertEquals(1, result.replyTurns().size());
        assertEquals("elena", result.replyTurns().get(0).npcId());
    }

    // ── continueScene : un tour de PNJ sans message du joueur ──────────────────────────────────────

    @Test
    void continueScenePlaysARoundWithoutAnyPlayerTurn(@TempDir Path root) throws IOException {
        writeTwoNpcScenarioFiles(root, "inn");
        ChatServiceImpl service = newService(32_768);
        ChatSession session = service.openSession(root, service.loadScenario(root, "inn"), true);
        session.updateGenerationSettings(alwaysInterject());
        service.sendMessage(root, session, "Elena, what do you make of this?");
        int turnsBefore = session.turns().size();
        session.updateGenerationSettings(alwaysInterject()); // meme avec une chance certaine : pas d'interjection ici

        ChatTurnResult result = service.continueScene(root, session, ExchangeProgressListener.NOOP);

        assertEquals(null, result.playerTurn());
        assertEquals(1, result.replyTurns().size(), "un seul PNJ principal, jamais d'interjecteur");
        assertEquals(turnsBefore + 1, session.turns().size());
        assertEquals(ChatTurn.Speaker.LLM, session.turns().get(session.turns().size() - 1).speaker());
        assertEquals(result.replyTurns(), result.newTurns(), "rien d'autre a ajouter cote client");
    }

    @Test
    void continueSceneOnAnActiveNpcMakesItSpeakAndNobodyElse(@TempDir Path root) throws IOException {
        writeTwoNpcScenarioFiles(root, "inn");
        ChatServiceImpl service = newService(32_768);
        ChatSession session = service.openSession(root, service.loadScenario(root, "inn"), true);
        session.updateGenerationSettings(neverInterject());
        service.sendMessage(root, session, "Hello there.");
        // present -> passive -> absent -> actif (3 clics, voir mute())
        for (int click = 0; click < 3; click++) service.cycleNpcActivity(root, session, "marcus");
        assertEquals(storymagine.chat.coeur.domaine.activity.NpcActivity.ACTIVE, session.activity().levelOf("marcus"));

        ChatTurnResult result = service.continueScene(root, session, ExchangeProgressListener.NOOP);

        assertTrue(result.replyTurns().stream().anyMatch(t -> "marcus".equals(t.npcId())), "l'actif parle toujours");
    }

    @Test
    void continueSceneBeforeAnyoneHasSpokenIsRefused(@TempDir Path root) throws IOException {
        writeTwoNpcScenarioFiles(root, "inn");
        ChatServiceImpl service = newService(32_768);
        ChatSession session = service.openSession(root, service.loadScenario(root, "inn"), true);

        assertThrows(IllegalStateException.class,
            () -> service.continueScene(root, session, ExchangeProgressListener.NOOP));
    }

    @Test
    void retryAfterAContinuationGoesBackToTheLastRealPlayerMessage(@TempDir Path root) throws IOException {
        writeTwoNpcScenarioFiles(root, "inn");
        ChatServiceImpl service = newService(32_768);
        ChatSession session = service.openSession(root, service.loadScenario(root, "inn"), true);
        session.updateGenerationSettings(neverInterject());
        service.sendMessage(root, session, "Elena, hello.");
        service.continueScene(root, session, ExchangeProgressListener.NOOP);
        int replyTurnsBefore = session.turns().size() - 1; // tout sauf le message du joueur

        ChatTurnResult retried = service.retry(root, session);

        assertEquals(replyTurnsBefore, retried.replacedTurnCount(), "le tour et sa continuation sont refaits ensemble");
        assertEquals(ChatTurn.Speaker.PLAYER, session.turns().get(0).speaker());
    }

    @Test
    void theChanceMarkersOfTheStyleNeverReachTheModelAndEachLineIsDrawn(@TempDir Path root) throws IOException {
        writeTwoNpcScenarioFiles(root, "inn");
        Files.writeString(root.resolve("inn/style.txt"), """
            Phrases courtes.
            - {20%} : un petit geste inconscient et anodin
            - {100%} : voix douce
            """, StandardCharsets.UTF_8);
        List<String> systemPrompts = new ArrayList<>();
        ModelCallPort capturing = new ModelCallPort() {
            @Override public LlmResult generate(String systemPrompt, String userPrompt, double temperature, LlmCallContext ctx) {
                systemPrompts.add(systemPrompt);
                return new LlmResult("grunts and nods slowly", 0, 0, 0, "");
            }
            @Override public int contextWindow() { return 32_768; }
        };
        StubModelCallPort other = new StubModelCallPort(32_768, "FOLDED", 0);
        Random drawsAt99Percent = new Random(1) { @Override public double nextDouble() { return 0.999; } };
        ChatServiceImpl service = new ChatServiceImpl(new ChatFileStorageAdapter(), new RoleplayNarrator(capturing),
            new ChatSummarizer(other), new NextActReadinessAnalyst(other), new NpcMindStateAnalyst(other),
            LogPort.NOOP, drawsAt99Percent);
        ChatSession session = service.openSession(root, service.loadScenario(root, "inn"), true);

        service.sendMessage(root, session, "Elena, what do you make of this?");

        String system = systemPrompts.get(0);
        assertFalse(system.contains("%"), system);
        assertFalse(system.contains("{"), system);
        assertFalse(system.contains("petit geste"), "tirage a 99.9 % : la ligne a 20 % disparait");
        assertTrue(system.contains("Phrases courtes."), system);
        assertTrue(system.contains("- : voix douce"), "la ligne a 100 % reste, sans son marqueur : " + system);
        // le debug 🐞 montre ce que le modele a vraiment recu, pas le fichier
        assertEquals("Phrases courtes.\n- : voix douce", service.scenarioPrompt(session).styleReceived().text());
        assertTrue(service.scenarioPrompt(session).style().contains("{20%}"), "le fichier, lui, garde ses marqueurs");
    }

    @Test
    void theDebugViewDoesNotWarnAboutTheLengthOfTheStyleFile(@TempDir Path root) throws IOException {
        writeTwoNpcScenarioFiles(root, "inn");
        Files.writeString(root.resolve("inn/style.txt"),
            "Phrases courtes.\n- {10%} " + "x".repeat(2_000) + "\n", StandardCharsets.UTF_8);
        ChatServiceImpl service = newService(32_768);
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, true);

        assertTrue(service.scenarioWarnings(scenario).stream().anyMatch(w -> w.contains("style.txt")),
            "le chargement et la validation avertissent toujours");
        assertTrue(service.scenarioPrompt(session).warnings().stream().noneMatch(w -> w.contains("style.txt")),
            "pas dans 🐞 : la taille du fichier n'est pas ce que le modele recoit");
    }

    @Test
    void eachSpeakerOfARoundGetsItsOwnDrawOfTheStyleChanceLines(@TempDir Path root) throws IOException {
        writeTwoNpcScenarioFiles(root, "inn");
        Files.writeString(root.resolve("inn/style.txt"), "Phrases courtes.\n- {20%} Un petit geste anodin.\n", StandardCharsets.UTF_8);
        List<String> systemPrompts = new ArrayList<>();
        ModelCallPort capturing = new ModelCallPort() {
            @Override public LlmResult generate(String systemPrompt, String userPrompt, double temperature, LlmCallContext ctx) {
                systemPrompts.add(systemPrompt);
                return new LlmResult("grunts and nods slowly", 0, 0, 0, "");
            }
            @Override public int contextWindow() { return 32_768; }
        };
        StubModelCallPort other = new StubModelCallPort(32_768, "FOLDED", 0);
        // nextDouble() dans l'ordre : le jet d'interjection de Marcus (passe), puis le tirage de style
        // d'Elena (0.0 : la ligne a 20 % reste), puis celui de Marcus (0.999 : elle disparait)
        double[] draws = {0.0, 0.0, 0.999};
        int[] next = {0};
        Random scripted = new Random(1) {
            @Override public double nextDouble() { return next[0] < draws.length ? draws[next[0]++] : 0.999; }
        };
        ChatServiceImpl service = new ChatServiceImpl(new ChatFileStorageAdapter(), new RoleplayNarrator(capturing),
            new ChatSummarizer(other), new NextActReadinessAnalyst(other), new NpcMindStateAnalyst(other),
            LogPort.NOOP, scripted);
        ChatSession session = service.openSession(root, service.loadScenario(root, "inn"), true);
        session.updateGenerationSettings(alwaysInterject());

        ChatTurnResult result = service.sendMessage(root, session, "Elena, what do you make of this?");

        assertEquals(List.of("elena", "marcus"), result.replyTurns().stream().map(ChatTurn::npcId).toList());
        assertEquals(2, systemPrompts.size());
        assertTrue(systemPrompts.get(0).contains("Un petit geste anodin."), "Elena : tirage 0.0, la ligne reste");
        assertFalse(systemPrompts.get(1).contains("petit geste"), "Marcus : tirage 0.999, la ligne disparait");
    }

    @Test
    void overManyRoundsTheSpeakersOfOneRoundDrawTheirStyleIndependently(@TempDir Path root) throws IOException {
        writeTwoNpcScenarioFiles(root, "inn");
        Files.writeString(root.resolve("inn/style.txt"), "Phrases courtes.\n- {50%} Un petit geste anodin.\n", StandardCharsets.UTF_8);
        List<String> systemPrompts = new ArrayList<>();
        ModelCallPort capturing = new ModelCallPort() {
            @Override public LlmResult generate(String systemPrompt, String userPrompt, double temperature, LlmCallContext ctx) {
                systemPrompts.add(systemPrompt);
                return new LlmResult("grunts and nods slowly", 0, 0, 0, "");
            }
            @Override public int contextWindow() { return 32_768; }
        };
        StubModelCallPort other = new StubModelCallPort(32_768, "FOLDED", 0);
        ChatServiceImpl service = new ChatServiceImpl(new ChatFileStorageAdapter(), new RoleplayNarrator(capturing),
            new ChatSummarizer(other), new NextActReadinessAnalyst(other), new NpcMindStateAnalyst(other),
            LogPort.NOOP, new Random(42));
        ChatSession session = service.openSession(root, service.loadScenario(root, "inn"), true);
        session.updateGenerationSettings(alwaysInterject()); // Elena puis Marcus, a chaque tour

        int rounds = 300;
        int differ = 0;
        int firstHasLine = 0;
        for (int i = 0; i < rounds; i++) {
            systemPrompts.clear();
            service.sendMessage(root, session, "Elena, what do you make of this?");
            assertEquals(2, systemPrompts.size(), "deux locuteurs a chaque tour");
            boolean first = systemPrompts.get(0).contains("petit geste");
            boolean second = systemPrompts.get(1).contains("petit geste");
            if (first != second) differ++;
            if (first) firstHasLine++;
            assertFalse(systemPrompts.get(0).contains("%") || systemPrompts.get(1).contains("%"));
        }

        // deux tirages independants a 50 % : les deux prompts different dans 1 tour sur 2
        assertEquals(0.5, differ / (double) rounds, 0.08, "tours ou les deux locuteurs n'ont pas la meme consigne");
        assertEquals(0.5, firstHasLine / (double) rounds, 0.08, "frequence de la ligne chez le premier locuteur");
    }

    @Test
    void certainInterjectionChanceMakesTheOtherEligiblePresentNpcReactAfterTheAddressedOne(@TempDir Path root) throws IOException {
        writeTwoNpcScenarioFiles(root, "inn");
        ChatServiceImpl service = newService(32_768);
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, true);
        session.updateGenerationSettings(alwaysInterject());

        ChatTurnResult result = service.sendMessage(root, session, "Elena, what do you make of this?");

        assertEquals(2, result.replyTurns().size(), "marcus interjecte en plus d'elena, chance certaine");
        assertEquals("elena", result.replyTurns().get(0).npcId(), "le PNJ vise repond en premier");
        assertEquals("marcus", result.replyTurns().get(1).npcId(), "l'interjecteur repond apres, a deja vu la reponse d'elena");
    }

    @Test
    void interjectorNextActTriggerIsIgnored(@TempDir Path root) throws IOException {
        writeTwoNpcScenarioFiles(root, "inn");
        writeActs(root, "inn", "First act.", "Second act.");
        SequencedModelCallPort roleplayLlm = new SequencedModelCallPort(32_768, List.of(
            "*nods* Elena stays put.", "*grins* Marcus can't help himself. [NEXT ACT]"));
        ChatServiceImpl service = new ChatServiceImpl(new ChatFileStorageAdapter(), new RoleplayNarrator(roleplayLlm),
            new ChatSummarizer(new StubModelCallPort(32_768, "FOLDED", 0)), new NextActReadinessAnalyst(roleplayLlm), new NpcMindStateAnalyst(roleplayLlm));
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, true);
        session.updateGenerationSettings(alwaysInterject());

        ChatTurnResult result = service.sendMessage(root, session, "Elena, what do you make of this?");

        assertEquals(2, result.replyTurns().size(), "marcus interjecte en plus d'elena, chance certaine");
        assertFalse(result.actAdvanced(), "le trigger [NEXT ACT] d'un interjecteur ne doit jamais avancer l'acte");
        assertEquals(1, session.currentAct());
    }

    @Test
    void zeroInterjectionChanceNeverMakesOthersReact(@TempDir Path root) throws IOException {
        writeTwoNpcScenarioFiles(root, "inn");
        ChatServiceImpl service = newService(32_768);
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, true);
        session.updateGenerationSettings(new GenerationSettings(null, null, null, null, null, null, null, 0.0, null));

        ChatTurnResult result = service.sendMessage(root, session, "Elena, what do you make of this?");

        assertEquals(1, result.replyTurns().size());
        assertEquals("elena", result.replyTurns().get(0).npcId());
    }

    @Test
    void reloadScenarioPicksUpFileEditsAndAddsNewNpcsPresentWithoutTouchingTurns(@TempDir Path root) throws IOException {
        writeTwoNpcScenarioFiles(root, "inn");
        ChatServiceImpl service = newService(32_768);
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, true);
        muteInSession(session, "marcus"); // elena present, marcus muted, before the reload
        List<ChatTurn> turnsBefore = session.turns();

        // Edite elena.txt et ajoute un troisieme PNJ pendant que la session tourne.
        Files.writeString(root.resolve("inn/characters/elena.txt"), "# Elena\nA reformed smuggler now.", StandardCharsets.UTF_8);
        Files.writeString(root.resolve("inn/characters/clara.txt"), "# Clara\nA traveling bard.", StandardCharsets.UTF_8);

        service.reloadScenario(root, session);

        assertEquals("A reformed smuggler now.",
            session.scenario().cast().find("elena").orElseThrow().publicInfo().lines().skip(1).findFirst().orElse(""));
        assertEquals(Set.of("elena", "clara"), session.presentNpcIds(), "marcus reste mute, clara nouvelle donc presente");
        assertEquals(turnsBefore, session.turns());

        ChatSession reloaded = newService(32_768).openSession(root, service.loadScenario(root, "inn"), false);
        assertEquals(Set.of("elena", "clara"), reloaded.presentNpcIds(), "la reconciliation est persistee");
    }

    @Test
    void cyclingAnNpcToAbsentPersistsAcrossReload(@TempDir Path root) throws IOException {
        writeTwoNpcScenarioFiles(root, "inn");
        ChatServiceImpl service = newService(32_768);
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, true);

        mute(service, root, session, "marcus");

        assertEquals(Set.of("elena"), session.presentNpcIds());
        ChatSession reloaded = newService(32_768).openSession(root, scenario, false);
        assertEquals(Set.of("elena"), reloaded.presentNpcIds());
    }

    @Test
    void cyclingTheOnlyNpcNeverEmptiesTheScene(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn");
        ChatServiceImpl service = newService(32_768);
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, true);

        for (int click = 0; click < 6; click++) {
            service.cycleNpcActivity(root, session, "character");
            assertEquals(Set.of("character"), session.presentNpcIds(), "clic " + (click + 1));
        }
    }

    // ── {npc-starts} : un PNJ ouvre l'histoire ou l'acte ───────────────────────

    private static GenerationSettings neverInterject() {
        return new GenerationSettings(null, null, null, null, null, null, null, 0.0, null);
    }

    /** Two clicks on a present NPC : present -> passive -> absent. */
    private static void mute(ChatServiceImpl service, Path root, ChatSession session, String npcId) {
        service.cycleNpcActivity(root, session, npcId);
        service.cycleNpcActivity(root, session, npcId);
    }

    private static void muteInSession(ChatSession session, String npcId) {
        session.cycleActivity(npcId);
        session.cycleActivity(npcId);
    }

    private static GenerationSettings alwaysInterject() {
        return new GenerationSettings(null, null, null, null, null, null, null, 100.0, null);
    }

    // ── {npc-mode do on|off} : le PNJ tient la plume du narrateur ──────────────

    private static void writeDoModeScenario(Path root, String actBody) throws IOException {
        writeTwoNpcScenarioFiles(root, "inn");
        Files.writeString(root.resolve("inn/scenario.txt"),
            "A stormy night at the inn.\n\n# Un\n" + actBody + "\nSomething is about to happen.\n", StandardCharsets.UTF_8);
    }

    @Test
    void aCharacterInDoModeReadsThePlayersLineAsADoAndTheOthersDoNot(@TempDir Path root) throws IOException {
        writeDoModeScenario(root, "{npc-mode do on Marcus}");
        LoggingModelCallPort llm = new LoggingModelCallPort();
        ChatServiceImpl service = newServiceWith(llm);
        ChatSession session = service.openSession(root, service.loadScenario(root, "inn"), true);
        session.updateGenerationSettings(neverInterject());

        service.sendMessage(root, session, "Marcus, what now?");
        service.sendMessage(root, session, "Elena, what do you think?");

        assertEquals(2, llm.userPrompts.size());
        assertTrue(llm.userPrompts.get(0).contains("DO: Marcus, what now?"), "Marcus tient la plume : " + llm.userPrompts.get(0));
        assertFalse(llm.userPrompts.get(1).contains("DO: Elena"), "Elena n'est pas en mode DO : " + llm.userPrompts.get(1));
        assertEquals("Marcus, what now?", session.turns().stream()
            .filter(t -> t.speaker() == ChatTurn.Speaker.PLAYER).findFirst().orElseThrow().text(),
            "l'historique et l'ecran gardent le texte du joueur tel qu'il l'a ecrit");
    }

    @Test
    void doModeLeavesOocLinesAndPlayerWrittenDoAlone(@TempDir Path root) throws IOException {
        writeDoModeScenario(root, "{npc-mode do on *}");
        LoggingModelCallPort llm = new LoggingModelCallPort();
        ChatServiceImpl service = newServiceWith(llm);
        ChatSession session = service.openSession(root, service.loadScenario(root, "inn"), true);
        session.updateGenerationSettings(neverInterject());

        service.sendMessage(root, session, "OOC: Marcus, be more careful");
        service.sendMessage(root, session, "DO: Marcus opens the door");

        assertFalse(llm.userPrompts.get(0).contains("DO: OOC:"), llm.userPrompts.get(0));
        assertFalse(llm.userPrompts.get(1).contains("DO: DO:"), llm.userPrompts.get(1));
    }

    @Test
    void doModeIsOffAgainAfterAnOffCommandAndNotAppliedWithoutAPlayerLine(@TempDir Path root) throws IOException {
        writeDoModeScenario(root, "{npc-mode do on *}\n{npc-mode do off Marcus}");
        LoggingModelCallPort llm = new LoggingModelCallPort();
        ChatServiceImpl service = newServiceWith(llm);
        ChatSession session = service.openSession(root, service.loadScenario(root, "inn"), true);
        session.updateGenerationSettings(neverInterject());

        assertEquals(java.util.Set.of("elena"), session.npcIdsInMode(storymagine.chat.coeur.domaine.activity.NpcMode.DO));
        service.sendMessage(root, session, "Marcus, what now?");

        assertFalse(llm.userPrompts.get(0).contains("DO: Marcus"), "off annule le on qui precede : " + llm.userPrompts.get(0));
    }

    // ── {npc-starts} : ouverture normale ; {npc-do} : ouverture avec la plume ──

    @Test
    void npcStartsOpensTheSceneWithNoPlayerLineAtAll(@TempDir Path root) throws IOException {
        writeTwoNpcScenarioFiles(root, "inn");
        Files.writeString(root.resolve("inn/scenario.txt"), "A stormy night at the inn.\n{npc-starts Marcus}\n", StandardCharsets.UTF_8);
        LoggingModelCallPort llm = new LoggingModelCallPort();
        ChatServiceImpl service = newServiceWith(llm);
        ChatSession session = service.openSession(root, service.loadScenario(root, "inn"), true);
        session.updateGenerationSettings(alwaysInterject()); // l'ouverture reste un PNJ seul, sans interjection

        assertFalse(session.pendingOpeningUsesPen(), "{npc-starts} est une ouverture normale");
        ChatTurnResult result = service.openScene(root, session, ExchangeProgressListener.NOOP);

        assertEquals(1, result.replyTurns().size());
        assertEquals("marcus", result.replyTurns().get(0).npcId());
        assertTrue(session.turns().stream().noneMatch(t -> t.speaker() == ChatTurn.Speaker.PLAYER), "aucune bulle DO: du joueur");
        assertTrue(session.pendingNpcOpening().isNone());
        assertThrows(IllegalStateException.class, () -> service.openScene(root, session, ExchangeProgressListener.NOOP));
    }

    @Test
    void npcDoOpensWithThePenAndSoDoesNpcStartsForACharacterInDoMode(@TempDir Path root) throws IOException {
        writeTwoNpcScenarioFiles(root, "inn");
        Files.writeString(root.resolve("inn/scenario.txt"), "A stormy night at the inn.\n{npc-do Marcus}\n", StandardCharsets.UTF_8);
        ChatServiceImpl service = newService(32_768);
        ChatSession doOpening = service.openSession(root, service.loadScenario(root, "inn"), true);
        assertTrue(doOpening.pendingOpeningUsesPen(), "{npc-do} : l'ouverture se fait avec la plume");

        Files.delete(root.resolve("inn/scenario.txt"));
        Files.writeString(root.resolve("inn/scenario.txt"),
            "A stormy night at the inn.\n\n# Un\n{npc-mode do on Marcus}\n{npc-starts Marcus}\nSomething.\n", StandardCharsets.UTF_8);
        service.restartSession(root, doOpening);
        assertTrue(doOpening.pendingOpeningUsesPen(), "{npc-starts Marcus} d'un PNJ en mode DO devient un DO");

        Files.writeString(root.resolve("inn/scenario.txt"),
            "A stormy night at the inn.\n\n# Un\n{npc-mode do on Elena}\n{npc-starts Marcus}\nSomething.\n", StandardCharsets.UTF_8);
        service.restartSession(root, doOpening);
        assertFalse(doOpening.pendingOpeningUsesPen(), "Marcus n'est pas en mode DO : ouverture normale");
    }

    @Test
    void severalNamedNpcsOpenTheStoryInTheOrderWrittenInOneRound(@TempDir Path root) throws IOException {
        writeTwoNpcScenarioFiles(root, "inn");
        Files.writeString(root.resolve("inn/scenario.txt"), "A stormy night at the inn.\n{npc-starts Marcus, Elena}\n", StandardCharsets.UTF_8);
        ChatServiceImpl service = newService(32_768);
        ChatSession session = service.openSession(root, service.loadScenario(root, "inn"), true);
        session.updateGenerationSettings(alwaysInterject());

        ChatTurnResult result = service.openScene(root, session, ExchangeProgressListener.NOOP);

        assertEquals(List.of("marcus", "elena"), result.replyTurns().stream().map(ChatTurn::npcId).toList());
        assertTrue(session.pendingNpcOpening().isNone(), "une fois la ronde jouee, plus rien n'est en attente");
    }

    @Test
    void aNpcNamedTwiceSpeaksTwiceInTheOpeningRound(@TempDir Path root) throws IOException {
        writeTwoNpcScenarioFiles(root, "inn");
        Files.writeString(root.resolve("inn/scenario.txt"), "A stormy night at the inn.\n{npc-starts Marcus, Elena, Marcus}\n", StandardCharsets.UTF_8);
        ChatServiceImpl service = newService(32_768);
        ChatSession session = service.openSession(root, service.loadScenario(root, "inn"), true);

        ChatTurnResult result = service.openScene(root, session, ExchangeProgressListener.NOOP);

        assertEquals(List.of("marcus", "elena", "marcus"), result.replyTurns().stream().map(ChatTurn::npcId).toList());
    }

    @Test
    void severalNpcsOpenWithThePenOnAnEmptyDoInTheOrderWritten(@TempDir Path root) throws IOException {
        writeTwoNpcScenarioFiles(root, "inn");
        Files.writeString(root.resolve("inn/scenario.txt"), "A stormy night at the inn.\n{npc-do Marcus, Elena}\n", StandardCharsets.UTF_8);
        ChatServiceImpl service = newService(32_768);
        ChatSession session = service.openSession(root, service.loadScenario(root, "inn"), true);
        session.updateGenerationSettings(alwaysInterject());

        ChatTurnResult result = service.sendMessage(root, session, "DO:");

        assertEquals(List.of("marcus", "elena"), result.replyTurns().stream().map(ChatTurn::npcId).toList());
    }

    @Test
    void emptyDoOpensTheStoryWithTheNpcNamedByTheMarkerAndNobodyElse(@TempDir Path root) throws IOException {
        writeTwoNpcScenarioFiles(root, "inn");
        Files.writeString(root.resolve("inn/scenario.txt"), "A stormy night at the inn.\n{npc-starts Marcus}\n", StandardCharsets.UTF_8);
        ChatServiceImpl service = newService(32_768);
        ChatSession session = service.openSession(root, service.loadScenario(root, "inn"), true);
        session.updateGenerationSettings(alwaysInterject()); // sans le garde-fou, Elena interviendrait a coup sur

        assertFalse(session.pendingNpcOpening().isNone());
        ChatTurnResult result = service.sendMessage(root, session, "DO:");

        assertEquals(1, result.replyTurns().size());
        assertEquals("marcus", result.replyTurns().get(0).npcId());
        assertTrue(session.pendingNpcOpening().isNone(), "une fois le PNJ intervenu, plus rien n'est en attente");
    }

    @Test
    void aGameOpenedByANpcIsOfferedForResumingAfterARestart(@TempDir Path root) throws IOException {
        writeTwoNpcScenarioFiles(root, "inn");
        Files.writeString(root.resolve("inn/scenario.txt"), "A stormy night at the inn.\n{npc-starts Marcus}\n", StandardCharsets.UTF_8);
        ChatServiceImpl service = newService(32_768);
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, false);
        assertFalse(service.hasSavedSession(root, scenario), "avant l'ouverture : rien a reprendre");

        service.sendMessage(root, session, "DO:");

        assertTrue(service.hasSavedSession(root, scenario), "l'ouverture par le PNJ est une partie a reprendre");
        ChatSession reopened = newService(32_768).openSession(root, scenario, false);
        assertTrue(reopened.pendingNpcOpening().isNone(), "reprise : le PNJ ne rouvre pas l'histoire une 2e fois");
    }

    @Test
    void retryOfTheOpeningGivesTheWordToTheSameNamedNpcAgain(@TempDir Path root) throws IOException {
        writeTwoNpcScenarioFiles(root, "inn");
        Files.writeString(root.resolve("inn/scenario.txt"), "A stormy night at the inn.\n{npc-starts Marcus}\n", StandardCharsets.UTF_8);
        ChatServiceImpl service = newService(32_768);
        ChatSession session = service.openSession(root, service.loadScenario(root, "inn"), true);
        service.sendMessage(root, session, "DO:");

        ChatTurnResult retried = service.retry(root, session);

        assertEquals(1, retried.replyTurns().size());
        assertEquals("marcus", retried.replyTurns().get(0).npcId());
    }

    @Test
    void anOpeningWithoutNameStillMakesExactlyOnePresentNpcSpeak(@TempDir Path root) throws IOException {
        writeTwoNpcScenarioFiles(root, "inn");
        Files.writeString(root.resolve("inn/scenario.txt"), "A stormy night at the inn.\n{npc-starts}\n", StandardCharsets.UTF_8);
        ChatServiceImpl service = newService(32_768);
        ChatSession session = service.openSession(root, service.loadScenario(root, "inn"), true);
        session.updateGenerationSettings(alwaysInterject());

        ChatTurnResult result = service.sendMessage(root, session, "DO:");

        assertEquals(1, result.replyTurns().size());
        assertTrue(Set.of("elena", "marcus").contains(result.replyTurns().get(0).npcId()));
    }

    @Test
    void aScenarioWithoutMarkerKeepsTheUsualBehaviourOfAnEmptyDo(@TempDir Path root) throws IOException {
        writeTwoNpcScenarioFiles(root, "inn");
        ChatServiceImpl service = newService(32_768);
        ChatSession session = service.openSession(root, service.loadScenario(root, "inn"), true);
        session.updateGenerationSettings(alwaysInterject());

        assertTrue(session.pendingNpcOpening().isNone());
        ChatTurnResult result = service.sendMessage(root, session, "DO:");

        assertEquals(2, result.replyTurns().size(), "pas d'ouverture voulue : le tour normal, interjection comprise");
    }

    @Test
    void aRealPlayerMessageOnAFreshStoryIsNotAnOpening(@TempDir Path root) throws IOException {
        writeTwoNpcScenarioFiles(root, "inn");
        Files.writeString(root.resolve("inn/scenario.txt"), "A stormy night at the inn.\n{npc-starts Marcus}\n", StandardCharsets.UTF_8);
        ChatServiceImpl service = newService(32_768);
        ChatSession session = service.openSession(root, service.loadScenario(root, "inn"), true);

        ChatTurnResult result = service.sendMessage(root, session, "Elena, a word please.");

        assertEquals("elena", result.replyTurns().get(0).npcId(), "le joueur a parle en premier : sa mention prime");
        assertTrue(session.pendingNpcOpening().isNone());
    }

    @Test
    void theMarkerOfAnActOpensThatActWhenItBegins(@TempDir Path root) throws IOException {
        writeTwoNpcScenarioFiles(root, "inn");
        Files.writeString(root.resolve("inn/scenario.txt"),
            "A stormy night.\n\n# One\nThe door is shut.\n\n# Two\n{npc-starts Elena}\nThe door bursts open.\n",
            StandardCharsets.UTF_8);
        ChatServiceImpl service = newService(32_768);
        ChatSession session = service.openSession(root, service.loadScenario(root, "inn"), true);
        session.updateGenerationSettings(alwaysInterject());

        assertTrue(session.pendingNpcOpening().isNone(), "l'acte 1 n'a pas de marqueur");
        service.sendMessage(root, session, "Hello.");
        assertTrue(service.advanceAct(root, session));

        assertTrue(session.pendingNpcOpening().isNamed());
        ChatTurnResult result = service.sendMessage(root, session, "DO:");

        assertEquals(1, result.replyTurns().size());
        assertEquals("elena", result.replyTurns().get(0).npcId());
        assertTrue(session.pendingNpcOpening().isNone());
    }

    @Test
    void aNamedNpcMutedByThePlayerDoesNotOpenInsteadOfSomeoneElsePresent(@TempDir Path root) throws IOException {
        writeTwoNpcScenarioFiles(root, "inn");
        Files.writeString(root.resolve("inn/scenario.txt"), "A stormy night at the inn.\n{npc-starts Marcus}\n", StandardCharsets.UTF_8);
        ChatServiceImpl service = newService(32_768);
        ChatSession session = service.openSession(root, service.loadScenario(root, "inn"), true);
        mute(service, root, session, "marcus");

        ChatTurnResult result = service.sendMessage(root, session, "DO:");

        assertEquals("elena", result.replyTurns().get(0).npcId());
    }

    /** Fixed answer, remembers the last user prompt it was given. */
    /** Keeps every user prompt received, in call order : one per speaking character. */
    private static class LoggingModelCallPort implements ModelCallPort {
        final List<String> userPrompts = new java.util.ArrayList<>();

        @Override
        public LlmResult generate(String systemPrompt, String userPrompt, double temperature, LlmCallContext ctx) {
            userPrompts.add(userPrompt);
            return LlmResult.of("*nods slowly*");
        }

        @Override
        public int contextWindow() { return 32_768; }
    }

    private ChatServiceImpl newServiceWith(ModelCallPort roleplayLlm) {
        StubModelCallPort other = new StubModelCallPort(32_768, "FOLDED SUMMARY", 0);
        return new ChatServiceImpl(new ChatFileStorageAdapter(), new RoleplayNarrator(roleplayLlm), new ChatSummarizer(other),
            new NextActReadinessAnalyst(other), new NpcMindStateAnalyst(other), new ReplyOriginAnalyst(other),
            LogPort.NOOP, new Random(1));
    }

    private static class RecordingModelCallPort implements ModelCallPort {
        private final String responseText;
        String lastUserPrompt;

        RecordingModelCallPort(String responseText) {
            this.responseText = responseText;
        }

        @Override
        public LlmResult generate(String systemPrompt, String userPrompt, double temperature, LlmCallContext ctx) {
            this.lastUserPrompt = userPrompt;
            return LlmResult.of(responseText);
        }

        @Override
        public int contextWindow() { return 32_768; }
    }

    private static class StubModelCallPort implements ModelCallPort {
        private final int    contextWindow;
        private final String responseText;
        private final int    promptTokens;

        StubModelCallPort(int contextWindow, String responseText, int promptTokens) {
            this.contextWindow = contextWindow;
            this.responseText  = responseText;
            this.promptTokens  = promptTokens;
        }

        @Override
        public LlmResult generate(String systemPrompt, String userPrompt, double temperature, LlmCallContext ctx) {
            return new LlmResult(responseText, promptTokens, 0, 0, "");
        }

        @Override
        public int contextWindow() { return contextWindow; }
    }

    /** Returns responses.get(0), then .get(1), etc. in call order (last one repeats past the end) —
     *  for tests where different speakers in the same round must get different replies. */
    private static class SequencedModelCallPort implements ModelCallPort {
        private final int          contextWindow;
        private final List<String> responses;
        private int                callIndex = 0;

        SequencedModelCallPort(int contextWindow, List<String> responses) {
            this.contextWindow = contextWindow;
            this.responses     = responses;
        }

        @Override
        public LlmResult generate(String systemPrompt, String userPrompt, double temperature, LlmCallContext ctx) {
            String text = responses.get(Math.min(callIndex, responses.size() - 1));
            callIndex++;
            return new LlmResult(text, 0, 0, 0, "");
        }

        @Override
        public int contextWindow() { return contextWindow; }
    }

    /** Reports each of partialFragments via onPartial before returning finalText — simulates a
     *  streaming Ollama call for ModelCallPort.generate(..., Consumer) (see OllamaAdapter). */
    private static class StreamingStubModelCallPort implements ModelCallPort {
        private final int          contextWindow;
        private final List<String> partialFragments;
        private final String       finalText;

        StreamingStubModelCallPort(int contextWindow, List<String> partialFragments, String finalText) {
            this.contextWindow    = contextWindow;
            this.partialFragments = partialFragments;
            this.finalText        = finalText;
        }

        @Override
        public LlmResult generate(String systemPrompt, String userPrompt, double temperature, LlmCallContext ctx) {
            return new LlmResult(finalText, 0, 0, 0, "");
        }

        @Override
        public LlmResult generate(String systemPrompt, String userPrompt, double temperature, LlmCallContext ctx,
                                   GenerationOptions options, java.util.function.Consumer<PartialGeneration> onPartial) {
            partialFragments.forEach(fragment -> onPartial.accept(new PartialGeneration("", fragment)));
            return new LlmResult(finalText, 0, 0, 0, "");
        }

        @Override
        public int contextWindow() { return contextWindow; }
    }

    private static class CancellingModelCallPort implements ModelCallPort {
        @Override
        public LlmResult generate(String systemPrompt, String userPrompt, double temperature, LlmCallContext ctx) {
            throw new GenerationCancelledException("stopped for test");
        }

        @Override
        public int contextWindow() { return 32_768; }
    }

    /** Succeeds successesBeforeCancel times, then throws GenerationCancelledException on every call after — simulates
     *  a Stop landing partway through a multi-speaker round. */
    private static class SucceedThenCancelModelCallPort implements ModelCallPort {
        private final int    successesBeforeCancel;
        private final String replyText;
        private int calls = 0;

        SucceedThenCancelModelCallPort(int successesBeforeCancel, String replyText) {
            this.successesBeforeCancel = successesBeforeCancel;
            this.replyText             = replyText;
        }

        @Override
        public LlmResult generate(String systemPrompt, String userPrompt, double temperature, LlmCallContext ctx) {
            calls++;
            if (calls > successesBeforeCancel) throw new GenerationCancelledException("stopped for test");
            return new LlmResult(replyText, 0, 0, 0, "");
        }

        @Override
        public int contextWindow() { return 32_768; }
    }
}
