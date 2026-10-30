package storymagine.chat.infra;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Knows what a scenario directory looked like BEFORE the characters/ + run/ structure : session
 * files and generated directories sitting next to scenario.txt, character sheets at the root. Used
 * to refuse loading such a directory with a clear message (ChatFileStorageAdapter).
 */
final class LegacyLayout {

    /** Old session files, written at the root of the scenario directory. */
    static final Set<String> SESSION_FILES = Set.of(
        "history.md", "summary.md", "act.txt", "present.txt", "interject.txt", "full-history.md");

    /** Old generated directories, at the root of the scenario directory. */
    static final Set<String> GENERATED_DIRS = Set.of("saves", "archive", "logs", "tests");

    private static final String SCENARIO_FILE = "scenario.txt";
    /** The optional style advice, a root file that may well start with a "# Title" without being a character sheet. */
    static final String STYLE_FILE = "style.txt";
    private static final Pattern SCENARIO_CONTINUATION = Pattern.compile("scenario-(\\d+)\\.txt");

    private LegacyLayout() {}

    static boolean isLegacy(Path scenarioDir) {
        for (String name : SESSION_FILES) {
            if (Files.exists(scenarioDir.resolve(name))) return true;
        }
        for (String name : GENERATED_DIRS) {
            if (Files.isDirectory(scenarioDir.resolve(name))) return true;
        }
        return !rootCharacterSheets(scenarioDir).isEmpty();
    }

    /** Root ".txt" files that are character sheets of the old layout : first line "# Name", not scenario.txt / scenario-N.txt. */
    static List<Path> rootCharacterSheets(Path scenarioDir) {
        if (!Files.isDirectory(scenarioDir)) return List.of();
        try (var entries = Files.list(scenarioDir)) {
            return entries
                .filter(Files::isRegularFile)
                .filter(f -> f.getFileName().toString().endsWith(".txt"))
                .filter(f -> !f.getFileName().toString().equals(SCENARIO_FILE))
                .filter(f -> !f.getFileName().toString().equals(STYLE_FILE))
                .filter(f -> !SCENARIO_CONTINUATION.matcher(f.getFileName().toString()).matches())
                .filter(LegacyLayout::startsWithNameHeading)
                .sorted()
                .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static boolean startsWithNameHeading(Path file) {
        try {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            int newline = content.indexOf('\n');
            String firstLine = (newline < 0 ? content : content.substring(0, newline)).strip();
            return firstLine.startsWith("# ");
        } catch (IOException e) {
            throw new UncheckedIOException("Lecture impossible : " + file, e);
        }
    }
}
