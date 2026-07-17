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
import storymagine.chat.coeur.domaine.session.ChatTurn;
import storymagine.chat.coeur.domaine.session.GenerationSettings;
import storymagine.chat.coeur.domaine.session.SavePoint;
import storymagine.chat.coeur.domaine.session.Scene;
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
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("character.txt"), "A grumpy innkeeper.", StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("scenario.txt"), "A stormy night at the inn.", StandardCharsets.UTF_8);
    }

    /** elena.txt + marcus.txt (pas de character.txt) — scenario multi-PNJ pour SpeakerSelector. */
    private static void writeTwoNpcScenarioFiles(Path root, String name) throws IOException {
        Path dir = root.resolve(name);
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("elena.txt"), "# Elena\nA sharp-tongued merchant.", StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("marcus.txt"), "# Marcus\nA retired soldier.", StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("scenario.txt"), "A stormy night at the inn.", StandardCharsets.UTF_8);
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
    void noCompactionWhileAtOrBelowKeepRecentTurnsEvenIfContextIsTiny(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn");
        ChatServiceImpl service = newService(80); // fenetre minuscule -> budget dynamique clampe a 0, depasse au moindre tour
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, true);

        // 2 echanges = 4 tours = KEEP_RECENT_TURNS : jamais compacte, quelle que soit la taille du texte
        service.sendMessage(root, session, "one two three four five");
        ChatTurnResult second = service.sendMessage(root, session, "six seven eight nine ten");

        assertFalse(second.compacted());
        assertEquals(4, session.turns().size());
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
        assertEquals(4, session.turns().size(), "seuls les KEEP_RECENT_TURNS derniers tours restent");
        assertEquals("FOLDED SUMMARY", session.summary());

        // les tours replies ne sont pas perdus : ils survivent verbatim dans l'archive append-only
        String archived = Files.readString(root.resolve("inn/full-history.md"), StandardCharsets.UTF_8);
        assertTrue(archived.contains("one two three four five"), "premier tour replie, doit etre archive");
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
        assertFalse(Files.exists(root.resolve("inn/full-history.md")),
            "rien n'a ete replie, donc rien ne doit etre archive");
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

        NpcMindStateAnalystOutput result = service.analyzeMindState(root, session);

        assertEquals("She is behind the bar, alone with the player.", result.situation());
        assertEquals("She does not trust him yet.", result.thoughts());
        assertEquals("Keep the conversation short.", result.plans());
        assertEquals(turnsBefore, session.turns(), "lecture seule : aucun tour ajoute");
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
        service.setNpcPresent(root, session, "marcus", false);
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

        Files.writeString(root.resolve("inn").resolve("elena.txt"), "# Elena\nUpdated public sheet.",
            StandardCharsets.UTF_8);

        service.restartSession(root, session);

        assertTrue(session.scenario().cast().find("elena").orElseThrow().publicInfo().contains("Updated public sheet."),
            "la fiche relue depuis le disque doit refleter l'edition, pas l'ancienne version en memoire");
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
        // sans ca, marcus (present, eligible par defaut) tirerait sa chance d'interjection —
        // ce test porte sur SpeakerSelector.select() seul, pas sur rollInterjectors (teste a part).
        session.setInterjecting("marcus", false);

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
                    if ("elena".equals(turn.npcId())) session.setPresent("marcus", false);
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
        session.updateGenerationSettings(new GenerationSettings(null, null, null, null, null, null, null, 0.0));

        ChatTurnResult result = service.sendMessage(root, session, "Hello there.");

        assertEquals(1, result.replyTurns().size());
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
        session.updateGenerationSettings(new GenerationSettings(null, null, null, null, null, null, null, 1.0));

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
        session.setInterjecting("marcus", false); // isole select() : pas d'interjection ici
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
        service.setNpcPresent(root, session, "marcus", false);

        ChatTurnResult result = service.sendMessage(root, session, "Hello there.");

        assertEquals(1, result.replyTurns().size());
        assertEquals("elena", result.replyTurns().get(0).npcId());
    }

    @Test
    void certainInterjectionChanceMakesTheOtherEligiblePresentNpcReactAfterTheAddressedOne(@TempDir Path root) throws IOException {
        writeTwoNpcScenarioFiles(root, "inn");
        ChatServiceImpl service = newService(32_768);
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, true);
        session.updateGenerationSettings(new GenerationSettings(null, null, null, null, null, null, null, 1.0));

        ChatTurnResult result = service.sendMessage(root, session, "Elena, what do you make of this?");

        assertEquals(2, result.replyTurns().size(), "marcus interjecte en plus d'elena, chance certaine");
        assertEquals("elena", result.replyTurns().get(0).npcId(), "le PNJ vise repond en premier");
        assertEquals("marcus", result.replyTurns().get(1).npcId(), "l'interjecteur repond apres, a deja vu la reponse d'elena");
    }

    @Test
    void zeroInterjectionChanceNeverMakesOthersReact(@TempDir Path root) throws IOException {
        writeTwoNpcScenarioFiles(root, "inn");
        ChatServiceImpl service = newService(32_768);
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, true);
        session.updateGenerationSettings(new GenerationSettings(null, null, null, null, null, null, null, 0.0));

        ChatTurnResult result = service.sendMessage(root, session, "Elena, what do you make of this?");

        assertEquals(1, result.replyTurns().size());
        assertEquals("elena", result.replyTurns().get(0).npcId());
    }

    @Test
    void interjectionIsSkippedForANpcThatOptedOutEvenAtCertainChance(@TempDir Path root) throws IOException {
        writeTwoNpcScenarioFiles(root, "inn");
        ChatServiceImpl service = newService(32_768);
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, true);
        session.updateGenerationSettings(new GenerationSettings(null, null, null, null, null, null, null, 1.0));
        service.setNpcInterjecting(root, session, "marcus", false);

        ChatTurnResult result = service.sendMessage(root, session, "Elena, what do you make of this?");

        assertEquals(1, result.replyTurns().size(), "marcus a opte dehors, meme a chance certaine");
    }

    @Test
    void reloadScenarioPicksUpFileEditsAndAddsNewNpcsPresentWithoutTouchingTurns(@TempDir Path root) throws IOException {
        writeTwoNpcScenarioFiles(root, "inn");
        ChatServiceImpl service = newService(32_768);
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, true);
        session.setPresent("marcus", false); // elena present, marcus muted, before the reload
        List<ChatTurn> turnsBefore = session.turns();

        // Edite elena.txt et ajoute un troisieme PNJ pendant que la session tourne.
        Files.writeString(root.resolve("inn/elena.txt"), "# Elena\nA reformed smuggler now.", StandardCharsets.UTF_8);
        Files.writeString(root.resolve("inn/clara.txt"), "# Clara\nA traveling bard.", StandardCharsets.UTF_8);

        service.reloadScenario(root, session);

        assertEquals("A reformed smuggler now.",
            session.scenario().cast().find("elena").orElseThrow().publicInfo().lines().skip(1).findFirst().orElse(""));
        assertEquals(Set.of("elena", "clara"), session.presentNpcIds(), "marcus reste mute, clara nouvelle donc presente");
        assertEquals(turnsBefore, session.turns());

        ChatSession reloaded = newService(32_768).openSession(root, scenario, false);
        assertEquals(Set.of("elena", "clara"), reloaded.presentNpcIds(), "la reconciliation est persistee");
    }

    @Test
    void setNpcPresentPersistsAcrossReload(@TempDir Path root) throws IOException {
        writeTwoNpcScenarioFiles(root, "inn");
        ChatServiceImpl service = newService(32_768);
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, true);

        boolean changed = service.setNpcPresent(root, session, "marcus", false);

        assertTrue(changed);
        assertEquals(Set.of("elena"), session.presentNpcIds());
        ChatSession reloaded = newService(32_768).openSession(root, scenario, false);
        assertEquals(Set.of("elena"), reloaded.presentNpcIds());
    }

    @Test
    void setNpcPresentRefusesToMuteTheLastPresentNpc(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn");
        ChatServiceImpl service = newService(32_768);
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, true);

        boolean changed = service.setNpcPresent(root, session, "character", false);

        assertFalse(changed);
        assertEquals(Set.of("character"), session.presentNpcIds());
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
}
