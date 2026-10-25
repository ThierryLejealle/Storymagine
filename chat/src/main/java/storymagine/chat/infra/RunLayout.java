package storymagine.chat.infra;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Where everything a scenario generates while being played lives on disk (chatScenariosRoot/{name}/
 * run/...), and how the files of the run's sequence are named and recognised : a turn is
 * "{number:6}.md", a compaction is "{createdAfter:6}-compaction-f{foldedUpTo:6}.md" — the name alone
 * is enough to order the sequence and to find which compaction applies, without opening any file.
 * See specs/chat.md, "Le run".
 */
final class RunLayout {

    static final String RUN_DIR     = "run";
    static final String CURRENT_DIR = "current";
    static final String TURNS_DIR   = "turns";
    static final String STATE_FILE  = "state.txt";
    static final String RESOLVED_DIR = "resolved";
    static final String SAVES_DIR   = "saves";
    static final String ARCHIVE_DIR = "archive";
    static final String LOGS_DIR    = "logs";
    static final String TESTS_DIR   = "tests";

    private static final Pattern TURN_NAME       = Pattern.compile("(\\d{6,})\\.md");
    private static final Pattern COMPACTION_NAME = Pattern.compile("(\\d{6,})-compaction-f(\\d{6,})\\.md");

    private RunLayout() {}

    /** One file of turns/ : a turn (foldedUpTo unused, 0) or a compaction (number = createdAfter). */
    record SequenceFile(Kind kind, int number, int foldedUpTo, Path path) {
        enum Kind { TURN, COMPACTION }
    }

    static Path runDir(Path chatScenariosRoot, String scenarioName) {
        return chatScenariosRoot.resolve(scenarioName).resolve(RUN_DIR);
    }

    static Path currentDir(Path chatScenariosRoot, String scenarioName) {
        return runDir(chatScenariosRoot, scenarioName).resolve(CURRENT_DIR);
    }

    static Path turnsDir(Path chatScenariosRoot, String scenarioName) {
        return currentDir(chatScenariosRoot, scenarioName).resolve(TURNS_DIR);
    }

    static Path stateFile(Path chatScenariosRoot, String scenarioName) {
        return currentDir(chatScenariosRoot, scenarioName).resolve(STATE_FILE);
    }

    /** The scenario files with their PICK tags replaced — written once per run, see ChatFileStorageAdapter.scenarioTextDir. */
    static Path resolvedDir(Path chatScenariosRoot, String scenarioName) {
        return currentDir(chatScenariosRoot, scenarioName).resolve(RESOLVED_DIR);
    }

    static Path savesDir(Path chatScenariosRoot, String scenarioName) {
        return runDir(chatScenariosRoot, scenarioName).resolve(SAVES_DIR);
    }

    static Path archiveDir(Path chatScenariosRoot, String scenarioName) {
        return runDir(chatScenariosRoot, scenarioName).resolve(ARCHIVE_DIR);
    }

    static String turnFileName(int number) {
        return String.format("%06d.md", number);
    }

    static String compactionFileName(int createdAfter, int foldedUpTo) {
        return String.format("%06d-compaction-f%06d.md", createdAfter, foldedUpTo);
    }

    /** Empty if the file name is neither a turn nor a compaction file (anything else in turns/ is ignored). */
    static Optional<SequenceFile> parse(Path file) {
        String name = file.getFileName().toString();
        Matcher turn = TURN_NAME.matcher(name);
        if (turn.matches()) {
            return Optional.of(new SequenceFile(SequenceFile.Kind.TURN, Integer.parseInt(turn.group(1)), 0, file));
        }
        Matcher compaction = COMPACTION_NAME.matcher(name);
        if (compaction.matches()) {
            return Optional.of(new SequenceFile(SequenceFile.Kind.COMPACTION,
                Integer.parseInt(compaction.group(1)), Integer.parseInt(compaction.group(2)), file));
        }
        return Optional.empty();
    }

    /**
     * The run's files in sequence order : by number, a compaction right AFTER the turn it was made
     * after. Sorted from the parsed numbers, never from the alphabetical order of the names.
     */
    static List<SequenceFile> listSequence(Path turnsDir) {
        if (!Files.isDirectory(turnsDir)) return List.of();
        try (var entries = Files.list(turnsDir)) {
            return entries
                .filter(Files::isRegularFile)
                .map(RunLayout::parse)
                .flatMap(Optional::stream)
                .sorted(Comparator.comparingInt(SequenceFile::number).thenComparing(SequenceFile::kind))
                .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
