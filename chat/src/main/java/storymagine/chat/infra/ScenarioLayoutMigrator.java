package storymagine.chat.infra;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Brings scenario directories from the old layout (session files, saves, logs and character sheets
 * all next to scenario.txt) to the characters/ + run/ one. It deliberately does NOT convert old
 * games : character sheets are moved into characters/, and every old session file and generated
 * directory (history, summary, act, presence, full-history, saves, archive, logs, tests) is
 * deleted — only the scenario itself is kept. Two steps so a caller can show what is about to
 * happen and ask first : plan() touches nothing, execute() applies one scenario's plan. Idempotent :
 * a scenario already in the new layout has an empty plan. See specs/chat.md, "Le run".
 */
public final class ScenarioLayoutMigrator {

    private static final String SCENARIO_FILE  = "scenario.txt";
    private static final String CHARACTERS_DIR = "characters";

    /** One scenario directory's migration : what moves, what is deleted, what is left alone, and why it may be blocked. */
    public record ScenarioMigration(String scenarioName, Path scenarioDir, List<Path> characterSheetsToMove,
                                     List<Deletion> deletions, List<Path> leftInPlace, String conflict) {

        /** Nothing to move or delete, and not blocked : already in the new layout. */
        public boolean isUpToDate() {
            return characterSheetsToMove.isEmpty() && deletions.isEmpty() && conflict == null;
        }

        public boolean isBlocked() {
            return conflict != null;
        }
    }

    /** A file or directory (recursively) to delete, with its total size for the preview. */
    public record Deletion(Path path, long sizeBytes, boolean directory) {}

    /** Every scenario directory (one holding a scenario.txt) directly under chatScenariosRoot, sorted by name. */
    public List<ScenarioMigration> plan(Path chatScenariosRoot) {
        if (!Files.isDirectory(chatScenariosRoot)) return List.of();
        try (var entries = Files.list(chatScenariosRoot)) {
            return entries
                .filter(Files::isDirectory)
                .filter(dir -> Files.isRegularFile(dir.resolve(SCENARIO_FILE)))
                .sorted()
                .map(this::planScenario)
                .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private ScenarioMigration planScenario(Path dir) {
        List<Path> sheets = LegacyLayout.rootCharacterSheets(dir);
        String conflict = firstConflict(dir, sheets);

        List<Deletion> deletions = new ArrayList<>();
        for (String name : LegacyLayout.SESSION_FILES.stream().sorted().toList()) {
            Path file = dir.resolve(name);
            if (Files.isRegularFile(file)) deletions.add(new Deletion(file, sizeOf(file), false));
        }
        for (String name : LegacyLayout.GENERATED_DIRS.stream().sorted().toList()) {
            Path sub = dir.resolve(name);
            if (Files.isDirectory(sub)) deletions.add(new Deletion(sub, sizeOf(sub), true));
        }
        return new ScenarioMigration(dir.getFileName().toString(), dir, sheets, deletions,
            strayTxtFiles(dir, sheets), conflict);
    }

    /** A sheet whose destination in characters/ already exists : nothing is overwritten, the scenario is skipped. */
    private static String firstConflict(Path dir, List<Path> sheets) {
        for (Path sheet : sheets) {
            Path target = dir.resolve(CHARACTERS_DIR).resolve(sheet.getFileName());
            if (Files.exists(target)) {
                return "characters/" + sheet.getFileName() + " existe deja : rien n'est ecrase, scenario ignore.";
            }
        }
        return null;
    }

    /** Root .txt files that are neither the scenario text nor a sheet being moved — left where they are, only reported. */
    private static List<Path> strayTxtFiles(Path dir, List<Path> sheets) {
        try (var entries = Files.list(dir)) {
            return entries
                .filter(Files::isRegularFile)
                .filter(f -> f.getFileName().toString().endsWith(".txt"))
                .filter(f -> !f.getFileName().toString().equals(SCENARIO_FILE))
                .filter(f -> !f.getFileName().toString().matches("scenario-\\d+\\.txt"))
                .filter(f -> !sheets.contains(f))
                .filter(f -> !LegacyLayout.SESSION_FILES.contains(f.getFileName().toString()))
                .sorted()
                .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Applies one scenario's plan. A blocked scenario is never touched (IllegalStateException if asked anyway). */
    public void execute(ScenarioMigration migration) {
        if (migration.isBlocked()) {
            throw new IllegalStateException("Scenario bloque : " + migration.scenarioName() + " — " + migration.conflict());
        }
        try {
            if (!migration.characterSheetsToMove().isEmpty()) {
                Files.createDirectories(migration.scenarioDir().resolve(CHARACTERS_DIR));
            }
            for (Path sheet : migration.characterSheetsToMove()) {
                Files.move(sheet, migration.scenarioDir().resolve(CHARACTERS_DIR).resolve(sheet.getFileName()));
            }
            for (Deletion deletion : migration.deletions()) {
                if (deletion.directory()) RunArchiver.deleteRecursively(deletion.path());
                else Files.deleteIfExists(deletion.path());
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Migration impossible pour " + migration.scenarioName(), e);
        }
    }

    private static long sizeOf(Path path) {
        try (var paths = Files.walk(path)) {
            return paths.filter(Files::isRegularFile).mapToLong(ScenarioLayoutMigrator::fileSize).sum();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static long fileSize(Path file) {
        try {
            return Files.size(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
