package storymagine.chat.infra;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import storymagine.chat.infra.ScenarioLayoutMigrator.ScenarioMigration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScenarioLayoutMigratorTest {

    private final ScenarioLayoutMigrator migrator = new ScenarioLayoutMigrator();

    private static void write(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    /** A scenario exactly as the old layout left it : sheets and session files all next to scenario.txt. */
    private static Path oldScenario(Path root, String name) throws IOException {
        Path dir = root.resolve(name);
        write(dir.resolve("scenario.txt"), "premise");
        write(dir.resolve("scenario-1.txt"), "# Second\nact");
        write(dir.resolve("elena.txt"), "# Elena\nmerchant");
        write(dir.resolve("marcus.txt"), "# Marcus\nsoldier");
        write(dir.resolve("history.md"), "### PLAYER\nHi.\n");
        write(dir.resolve("summary.md"), "summary");
        write(dir.resolve("act.txt"), "1");
        write(dir.resolve("present.txt"), "elena");
        write(dir.resolve("interject.txt"), "elena");
        write(dir.resolve("full-history.md"), "old turns");
        write(dir.resolve("saves/2026-01-01-000000-000/history.md"), "saved");
        write(dir.resolve("archive/2026-01-01-000000-act.txt"), "1");
        write(dir.resolve("logs/2026-01-01_10h00/master-log.txt"), "log");
        write(dir.resolve("tests/2026-01-01_10h00/report.html"), "<html/>");
        return dir;
    }

    @Test
    void planListsEverySheetToMoveAndEveryOldFileAndDirectoryToDelete(@TempDir Path root) throws IOException {
        oldScenario(root, "inn");

        ScenarioMigration plan = migrator.plan(root).get(0);

        assertEquals("inn", plan.scenarioName());
        assertEquals(List.of("elena.txt", "marcus.txt"), plan.characterSheetsToMove().stream().map(p -> p.getFileName().toString()).toList());
        assertEquals(10, plan.deletions().size(), "6 fichiers de session + saves, archive, logs, tests");
        assertFalse(plan.isUpToDate());
        assertFalse(plan.isBlocked());
    }

    @Test
    void planTouchesNothing(@TempDir Path root) throws IOException {
        Path dir = oldScenario(root, "inn");

        migrator.plan(root);

        assertTrue(Files.exists(dir.resolve("history.md")));
        assertTrue(Files.exists(dir.resolve("elena.txt")));
        assertFalse(Files.exists(dir.resolve("characters")));
    }

    @Test
    void executeMovesTheSheetsIntoCharactersAndDeletesEveryOldGame(@TempDir Path root) throws IOException {
        Path dir = oldScenario(root, "inn");

        migrator.execute(migrator.plan(root).get(0));

        assertTrue(Files.exists(dir.resolve("characters/elena.txt")));
        assertTrue(Files.exists(dir.resolve("characters/marcus.txt")));
        assertFalse(Files.exists(dir.resolve("elena.txt")));
        for (String gone : List.of("history.md", "summary.md", "act.txt", "present.txt", "interject.txt",
            "full-history.md", "saves", "archive", "logs", "tests")) {
            assertFalse(Files.exists(dir.resolve(gone)), gone + " doit etre supprime");
        }
    }

    @Test
    void executeNeverTouchesTheScenarioItself(@TempDir Path root) throws IOException {
        Path dir = oldScenario(root, "inn");

        migrator.execute(migrator.plan(root).get(0));

        assertEquals("premise", Files.readString(dir.resolve("scenario.txt")));
        assertEquals("# Second\nact", Files.readString(dir.resolve("scenario-1.txt")));
    }

    @Test
    void aMigratedScenarioLoadsAsAValidNewLayoutScenario(@TempDir Path root) throws IOException {
        oldScenario(root, "inn");
        migrator.execute(migrator.plan(root).get(0));

        var scenario = new ChatFileStorageAdapter().loadScenario(root, "inn");

        assertEquals(2, scenario.cast().npcs().size());
    }

    @Test
    void migratingTwiceChangesNothingTheSecondTime(@TempDir Path root) throws IOException {
        oldScenario(root, "inn");
        migrator.execute(migrator.plan(root).get(0));

        ScenarioMigration again = migrator.plan(root).get(0);

        assertTrue(again.isUpToDate());
    }

    @Test
    void aScenarioAlreadyInTheNewLayoutIsUpToDateAndItsRunIsNotTouched(@TempDir Path root) throws IOException {
        Path dir = root.resolve("fresh");
        write(dir.resolve("scenario.txt"), "premise");
        write(dir.resolve("characters/elena.txt"), "# Elena\nmerchant");
        write(dir.resolve("run/current/turns/000001.md"), "a turn");

        ScenarioMigration plan = migrator.plan(root).get(0);

        assertTrue(plan.isUpToDate());
        assertTrue(Files.exists(dir.resolve("run/current/turns/000001.md")));
    }

    @Test
    void aSheetWhoseDestinationAlreadyExistsBlocksTheScenarioAndNothingIsOverwritten(@TempDir Path root) throws IOException {
        Path dir = oldScenario(root, "inn");
        write(dir.resolve("characters/elena.txt"), "# Elena\nthe NEW sheet");

        ScenarioMigration plan = migrator.plan(root).get(0);

        assertTrue(plan.isBlocked());
        assertThrows(IllegalStateException.class, () -> migrator.execute(plan));
        assertEquals("# Elena\nthe NEW sheet", Files.readString(dir.resolve("characters/elena.txt")));
        assertTrue(Files.exists(dir.resolve("history.md")), "un scenario bloque n'est pas touche du tout");
    }

    @Test
    void aTxtFileWithoutANameHeadingIsLeftInPlaceAndReported(@TempDir Path root) throws IOException {
        Path dir = oldScenario(root, "inn");
        write(dir.resolve("notes.txt"), "just my notes");

        ScenarioMigration plan = migrator.plan(root).get(0);
        migrator.execute(plan);

        assertEquals(List.of("notes.txt"), plan.leftInPlace().stream().map(p -> p.getFileName().toString()).toList());
        assertTrue(Files.exists(dir.resolve("notes.txt")));
    }

    @Test
    void aDirectoryWithoutAScenarioFileIsNotAScenarioAndIsIgnored(@TempDir Path root) throws IOException {
        write(root.resolve("not-a-scenario/history.md"), "x");

        assertTrue(migrator.plan(root).isEmpty());
        assertTrue(Files.exists(root.resolve("not-a-scenario/history.md")));
    }

    @Test
    void deletionsCarryTheirSizeForThePreview(@TempDir Path root) throws IOException {
        Path dir = oldScenario(root, "inn");
        write(dir.resolve("history.md"), "x".repeat(100));

        ScenarioMigration plan = migrator.plan(root).get(0);

        long historySize = plan.deletions().stream()
            .filter(d -> d.path().getFileName().toString().equals("history.md")).findFirst().orElseThrow().sizeBytes();
        assertEquals(100, historySize);
    }
}
