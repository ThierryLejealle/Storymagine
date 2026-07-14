package storymagine.chat.infra;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import storymagine.chat.coeur.domaine.scenario.ChatScenario;
import storymagine.chat.coeur.domaine.session.ChatSession;
import storymagine.chat.coeur.domaine.session.ChatTurn;
import storymagine.chat.coeur.domaine.session.SavePoint;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatFileStorageAdapterTest {

    private final ChatFileStorageAdapter adapter = new ChatFileStorageAdapter();

    @Test
    void listScenarioNamesOnlyKeepsDirsWithBothFiles(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "complete", "sheet", "premise");
        Files.createDirectories(root.resolve("incomplete"));
        Files.writeString(root.resolve("incomplete/character.txt"), "sheet only", StandardCharsets.UTF_8);

        assertEquals(List.of("complete"), adapter.listScenarioNames(root));
    }

    @Test
    void loadScenarioReadsBothFilesVerbatim(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "A grumpy innkeeper.", "A stormy night.");

        ChatScenario scenario = adapter.loadScenario(root, "inn");

        assertEquals("inn", scenario.name());
        assertEquals("A grumpy innkeeper.", scenario.characterSheet());
        assertEquals("A stormy night.", scenario.premise());
        assertTrue(scenario.acts().isEmpty());
    }

    @Test
    void loadScenarioReadsActHeadingsInDocumentOrder(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        appendActs(root, "inn", "First act.", "Second act.");

        ChatScenario scenario = adapter.loadScenario(root, "inn");

        assertEquals(2, scenario.acts().size());
        assertEquals("First act.", scenario.acts().get(0).text());
        assertEquals("Second act.", scenario.acts().get(1).text());
    }

    @Test
    void loadScenarioTakesTheActTitleFromItsHeadingLine(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        appendRawToScenario(root, "inn", "# The searched house\n[The door creaks.]\n\nBody text.");

        ChatScenario scenario = adapter.loadScenario(root, "inn");

        assertEquals("The searched house", scenario.acts().get(0).title());
        assertEquals("[The door creaks.]\n\nBody text.", scenario.acts().get(0).text());
    }

    @Test
    void loadScenarioLeavesTitleEmptyWhenHeadingHasNoTitleText(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        appendRawToScenario(root, "inn", "# \nBody text with no title.");

        ChatScenario scenario = adapter.loadScenario(root, "inn");

        assertEquals("", scenario.acts().get(0).title());
        assertEquals("Body text with no title.", scenario.acts().get(0).text());
    }

    @Test
    void loadSessionWithNoHistoryYetIsEmpty(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        ChatScenario scenario = adapter.loadScenario(root, "inn");

        ChatSession session = adapter.loadSession(root, scenario);

        assertTrue(session.turns().isEmpty());
        assertEquals("", session.summary());
        assertEquals(0, session.currentAct());
    }

    @Test
    void loadSessionWithActsAndNoPersistedStateDefaultsToActOne(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        appendActs(root, "inn", "First act.");
        ChatScenario scenario = adapter.loadScenario(root, "inn");

        ChatSession session = adapter.loadSession(root, scenario);

        assertEquals(1, session.currentAct());
    }

    @Test
    void saveThenLoadRoundTripsMultilineTurnsSummaryAndCurrentAct(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        appendActs(root, "inn", "First act.", "Second act.");
        ChatScenario scenario = adapter.loadScenario(root, "inn");

        // Un tour avec des lignes vides internes — le cas qui casse un parsing naif base sur
        // "un tour = une ligne".
        ChatSession session = new ChatSession(scenario, List.of(
            new ChatTurn(ChatTurn.Speaker.PLAYER, "*enters*\n\nHello there."),
            new ChatTurn(ChatTurn.Speaker.LLM, "*grunts*\nWhat do you want?")
        ), "They met at the door.", 2);

        adapter.saveSession(root, session);
        ChatSession reloaded = adapter.loadSession(root, scenario);

        assertEquals(session.turns(), reloaded.turns());
        assertEquals("They met at the door.", reloaded.summary());
        assertEquals(2, reloaded.currentAct());
    }

    @Test
    void saveThenLoadRoundTripsNarratorTurns(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        ChatScenario scenario = adapter.loadScenario(root, "inn");

        ChatSession session = new ChatSession(scenario, List.of(
            new ChatTurn(ChatTurn.Speaker.NARRATOR, "The door creaks open."),
            new ChatTurn(ChatTurn.Speaker.PLAYER, "Hi."),
            new ChatTurn(ChatTurn.Speaker.LLM, "Hello.")
        ), "", 0);

        adapter.saveSession(root, session);
        ChatSession reloaded = adapter.loadSession(root, scenario);

        assertEquals(session.turns(), reloaded.turns());
    }

    @Test
    void resetSessionArchivesHistorySummaryAndActInsteadOfDeletingButKeepsScenarioFiles(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        ChatScenario scenario = adapter.loadScenario(root, "inn");
        ChatSession session = new ChatSession(scenario, List.of(new ChatTurn(ChatTurn.Speaker.PLAYER, "Hi.")), "summary", 0);
        adapter.saveSession(root, session);

        adapter.resetSession(root, scenario);

        assertFalse(Files.exists(root.resolve("inn/history.md")));
        assertFalse(Files.exists(root.resolve("inn/summary.md")));
        assertFalse(Files.exists(root.resolve("inn/act.txt")));
        assertTrue(Files.exists(root.resolve("inn/character.txt")));
        ChatSession reloaded = adapter.loadSession(root, scenario);
        assertTrue(reloaded.turns().isEmpty());
        assertEquals("", reloaded.summary());

        // pas de perte : history/summary/act archives, pas supprimes
        try (var archived = Files.list(root.resolve("inn/archive"))) {
            List<String> names = archived.map(p -> p.getFileName().toString()).toList();
            assertEquals(3, names.size());
            assertTrue(names.stream().anyMatch(n -> n.endsWith("history.md")));
            assertTrue(names.stream().anyMatch(n -> n.endsWith("summary.md")));
            assertTrue(names.stream().anyMatch(n -> n.endsWith("act.txt")));
        }
    }

    @Test
    void resetSessionNeverTouchesSavePoints(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        ChatScenario scenario = adapter.loadScenario(root, "inn");
        ChatSession session = new ChatSession(scenario, List.of(new ChatTurn(ChatTurn.Speaker.PLAYER, "Hi.")), "", 0);
        adapter.saveSession(root, session);
        SavePoint save = adapter.createSavePoint(root, session);

        adapter.resetSession(root, scenario);

        assertEquals(List.of(save), adapter.listSavePoints(root, scenario));
        ChatSession reloadedSave = adapter.loadSavePoint(root, scenario, save.id());
        assertEquals(session.turns(), reloadedSave.turns());
    }

    @Test
    void archiveFoldedTurnsAppendsAcrossCallsInsteadOfOverwriting(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        ChatScenario scenario = adapter.loadScenario(root, "inn");

        adapter.archiveFoldedTurns(root, scenario, List.of(new ChatTurn(ChatTurn.Speaker.PLAYER, "First batch.")));
        adapter.archiveFoldedTurns(root, scenario, List.of(new ChatTurn(ChatTurn.Speaker.PLAYER, "Second batch.")));

        String archived = Files.readString(root.resolve("inn/full-history.md"), StandardCharsets.UTF_8);
        assertTrue(archived.contains("First batch."));
        assertTrue(archived.contains("Second batch."));
    }

    @Test
    void archiveFoldedTurnsIsANoOpForAnEmptyList(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        ChatScenario scenario = adapter.loadScenario(root, "inn");

        adapter.archiveFoldedTurns(root, scenario, List.of());

        assertFalse(Files.exists(root.resolve("inn/full-history.md")));
    }

    @Test
    void createSavePointThenLoadSavePointRoundTripsTurnsSummaryAndCurrentAct(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        appendActs(root, "inn", "First act.", "Second act.");
        ChatScenario scenario = adapter.loadScenario(root, "inn");
        ChatSession session = new ChatSession(scenario,
            List.of(new ChatTurn(ChatTurn.Speaker.PLAYER, "Hi.")), "Summary so far.", 2);

        SavePoint save = adapter.createSavePoint(root, session);
        ChatSession reloaded = adapter.loadSavePoint(root, scenario, save.id());

        assertEquals(session.turns(), reloaded.turns());
        assertEquals("Summary so far.", reloaded.summary());
        assertEquals(2, reloaded.currentAct());
    }

    @Test
    void createSavePointNeverTouchesTheLiveSessionFiles(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        ChatScenario scenario = adapter.loadScenario(root, "inn");
        ChatSession live = new ChatSession(scenario, List.of(new ChatTurn(ChatTurn.Speaker.PLAYER, "Live.")), "", 0);
        adapter.saveSession(root, live);

        adapter.createSavePoint(root, new ChatSession(scenario,
            List.of(new ChatTurn(ChatTurn.Speaker.PLAYER, "Snapshot.")), "", 0));

        ChatSession reloadedLive = adapter.loadSession(root, scenario);
        assertEquals(live.turns(), reloadedLive.turns());
    }

    @Test
    void listSavePointsReturnsNewestFirst(@TempDir Path root) throws IOException, InterruptedException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        ChatScenario scenario = adapter.loadScenario(root, "inn");
        ChatSession session = new ChatSession(scenario, List.of(), "", 0);

        SavePoint first = adapter.createSavePoint(root, session);
        Thread.sleep(5);
        SavePoint second = adapter.createSavePoint(root, session);

        assertEquals(List.of(second, first), adapter.listSavePoints(root, scenario));
    }

    @Test
    void listSavePointsIsEmptyWhenNoneWereEverCreated(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        ChatScenario scenario = adapter.loadScenario(root, "inn");

        assertEquals(List.of(), adapter.listSavePoints(root, scenario));
    }

    @Test
    void loadSavePointRejectsAnIdThatIsNotAPlainTimestamp(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        ChatScenario scenario = adapter.loadScenario(root, "inn");

        assertThrows(IllegalArgumentException.class,
            () -> adapter.loadSavePoint(root, scenario, "../../etc/passwd"));
    }

    @Test
    void deleteSavePointRemovesItFromTheListing(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        ChatScenario scenario = adapter.loadScenario(root, "inn");
        SavePoint save = adapter.createSavePoint(root, new ChatSession(scenario, List.of(), "", 0));

        adapter.deleteSavePoint(root, scenario, save.id());

        assertEquals(List.of(), adapter.listSavePoints(root, scenario));
    }

    @Test
    void deleteSavePointIsANoOpWhenTheIdDoesNotExist(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        ChatScenario scenario = adapter.loadScenario(root, "inn");

        adapter.deleteSavePoint(root, scenario, "2026-01-01-000000-000");

        assertEquals(List.of(), adapter.listSavePoints(root, scenario));
    }

    @Test
    void deleteSavePointRejectsAnIdThatIsNotAPlainTimestamp(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        ChatScenario scenario = adapter.loadScenario(root, "inn");

        assertThrows(IllegalArgumentException.class,
            () -> adapter.deleteSavePoint(root, scenario, "../../etc/passwd"));
    }

    private static void writeScenarioFiles(Path root, String name, String character, String scenarioText) throws IOException {
        Path dir = root.resolve(name);
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("character.txt"), character, StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("scenario.txt"), scenarioText, StandardCharsets.UTF_8);
    }

    /** Appends flat, top-level "# " act headings to the scenario's already-written scenario.txt. */
    private static void appendActs(Path root, String name, String... actBodies) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (String body : actBodies) sb.append("\n\n# \n").append(body);
        appendRawToScenario(root, name, sb.toString());
    }

    private static void appendRawToScenario(Path root, String name, String raw) throws IOException {
        Path file = root.resolve(name).resolve("scenario.txt");
        Files.writeString(file, Files.readString(file, StandardCharsets.UTF_8) + "\n\n" + raw, StandardCharsets.UTF_8);
    }
}
