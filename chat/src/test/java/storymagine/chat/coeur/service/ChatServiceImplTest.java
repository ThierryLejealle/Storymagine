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
import storymagine.chat.coeur.domaine.session.PlayerMessage;
import storymagine.chat.coeur.domaine.session.SavePoint;
import storymagine.chat.infra.ChatFileStorageAdapter;
import storymagine.commun.coeur.ports.GenerationCancelledException;
import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.LlmResult;
import storymagine.commun.coeur.ports.ModelCallPort;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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
        assertEquals(ChatTurn.Speaker.LLM, result.replyTurn().speaker());
        assertEquals("grunts and nods slowly", result.replyTurn().text());
        assertFalse(result.compacted());
        assertEquals(2, session.turns().size());

        // persiste : un nouveau service relisant le meme repertoire retrouve les 2 tours
        ChatSession reloaded = newService(32_768).openSession(root, scenario, false);
        assertEquals(2, reloaded.turns().size());
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
        assertEquals(result.replyTurn().text(), session.turns().get(1).text());
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

        ChatPromptBuilder.ChatPrompt next = ChatPromptBuilder.build(scenario, session.currentAct(), session.summary(),
            session.turns(), PlayerMessage.parse(""));
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
        assertFalse(result.replyTurn().text().contains("[NEXT ACT]"));
        assertEquals("*nods* Time to move on.", result.replyTurn().text());
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
        assertEquals(List.of(result.playerTurn(), result.replyTurn(),
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
    void deleteSavePointRemovesItFromTheListing(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn");
        ChatServiceImpl service = newService(32_768);
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, true);
        SavePoint save = service.save(root, session);

        service.deleteSavePoint(root, scenario, save.id());

        assertEquals(List.of(), service.listSavePoints(root, scenario));
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

    private static class CancellingModelCallPort implements ModelCallPort {
        @Override
        public LlmResult generate(String systemPrompt, String userPrompt, double temperature, LlmCallContext ctx) {
            throw new GenerationCancelledException("stopped for test");
        }

        @Override
        public int contextWindow() { return 32_768; }
    }
}
