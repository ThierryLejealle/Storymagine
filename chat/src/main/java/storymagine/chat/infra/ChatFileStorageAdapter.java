package storymagine.chat.infra;

import storymagine.chat.coeur.domaine.scenario.ChatScenario;
import storymagine.chat.coeur.domaine.session.ChatSession;
import storymagine.chat.coeur.domaine.session.ChatTurn;
import storymagine.chat.coeur.domaine.session.SavePoint;
import storymagine.chat.coeur.ports.ChatStoragePort;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * File-based ChatStoragePort : chatScenariosRoot/{name}/character.txt + scenario.txt (single file :
 * premise, optionally preceded by a "#SCENARIO" heading, followed by a nested Markdown outline of
 * acts — see ScenarioOutlineParser) and history.md + summary.md + act.txt (persisted session,
 * rewritten after every turn and on reset) + full-history.md (append-only archive of every turn
 * ever folded into the summary — never rewritten, never truncated). saves/{stamp}/ holds the same
 * three session files per player-triggered save point (see createSavePoint), never touched by the
 * automatic save/reset flow above. See ChatStoragePort for the directory convention.
 */
public class ChatFileStorageAdapter implements ChatStoragePort {

    private static final String CHARACTER_FILE = "character.txt";
    private static final String SCENARIO_FILE  = "scenario.txt";
    private static final String HISTORY_FILE      = "history.md";
    private static final String SUMMARY_FILE      = "summary.md";
    private static final String ACT_FILE          = "act.txt";
    private static final String FULL_HISTORY_FILE = "full-history.md";

    private static final String PLAYER_HEADER   = "### PLAYER";
    private static final String LLM_HEADER      = "### LLM";
    private static final String NARRATOR_HEADER = "### NARRATOR";

    private static final String ARCHIVE_DIR   = "archive";
    private static final DateTimeFormatter ARCHIVE_STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss");

    /** Millisecond precision (unlike ARCHIVE_STAMP) : createSavePoint can be called twice in quick
     *  succession (the safety-net save just before loadSavePoint overwrites the live session), and
     *  two saves landing in the same directory would silently overwrite one another. */
    private static final String SAVES_DIR = "saves";
    private static final DateTimeFormatter SAVE_STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss-SSS");
    private static final Pattern SAVE_ID_PATTERN = Pattern.compile("[0-9-]+");

    @Override
    public List<String> listScenarioNames(Path chatScenariosRoot) {
        if (!Files.isDirectory(chatScenariosRoot)) return List.of();
        try (var entries = Files.list(chatScenariosRoot)) {
            return entries
                .filter(Files::isDirectory)
                .filter(dir -> Files.exists(dir.resolve(CHARACTER_FILE)) && Files.exists(dir.resolve(SCENARIO_FILE)))
                .map(dir -> dir.getFileName().toString())
                .sorted()
                .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public ChatScenario loadScenario(Path chatScenariosRoot, String name) {
        Path dir = chatScenariosRoot.resolve(name);
        String characterSheet = readText(dir.resolve(CHARACTER_FILE));
        ScenarioOutlineParser.Outline outline = ScenarioOutlineParser.parse(readText(dir.resolve(SCENARIO_FILE)));
        return new ChatScenario(name, characterSheet, outline.premise(), outline.acts(),
            extractCharacterName(characterSheet));
    }

    private static final String NAME_HEADING_PREFIX = "# ";

    /** Optional "# Name" first line of character.txt — kept in characterSheet, not stripped. */
    private static String extractCharacterName(String characterSheet) {
        int newline = characterSheet.indexOf('\n');
        String firstLine = (newline < 0 ? characterSheet : characterSheet.substring(0, newline)).strip();
        return firstLine.startsWith(NAME_HEADING_PREFIX) ? firstLine.substring(NAME_HEADING_PREFIX.length()).strip() : "";
    }

    @Override
    public ChatSession loadSession(Path chatScenariosRoot, ChatScenario scenario) {
        return readSessionFiles(chatScenariosRoot.resolve(scenario.name()), scenario);
    }

    @Override
    public void saveSession(Path chatScenariosRoot, ChatSession session) {
        writeSessionFiles(chatScenariosRoot.resolve(session.scenario().name()), session);
    }

    /** Shared by loadSession and loadSavePoint — same three files, different directory. */
    private static ChatSession readSessionFiles(Path dir, ChatScenario scenario) {
        String summary = existsAndReadable(dir.resolve(SUMMARY_FILE)) ? readText(dir.resolve(SUMMARY_FILE)) : "";
        List<ChatTurn> turns = existsAndReadable(dir.resolve(HISTORY_FILE))
            ? parseHistory(readText(dir.resolve(HISTORY_FILE)))
            : List.of();
        int currentAct = existsAndReadable(dir.resolve(ACT_FILE))
            ? Integer.parseInt(readText(dir.resolve(ACT_FILE)).strip())
            : (scenario.acts().isEmpty() ? 0 : 1);
        return new ChatSession(scenario, turns, summary, currentAct);
    }

    /** Shared by saveSession and createSavePoint — same three files, different directory. */
    private static void writeSessionFiles(Path dir, ChatSession session) {
        writeText(dir.resolve(HISTORY_FILE), formatHistory(session.turns()));
        writeText(dir.resolve(SUMMARY_FILE), session.summary());
        writeText(dir.resolve(ACT_FILE), String.valueOf(session.currentAct()));
    }

    /**
     * Archives (never deletes) the previous session on reset — a long conversation is too costly
     * to lose to an accidental "n" at the "Continuer ?" prompt. Moved into archive/, timestamped.
     */
    @Override
    public void resetSession(Path chatScenariosRoot, ChatScenario scenario) {
        Path dir = chatScenariosRoot.resolve(scenario.name());
        String stamp = LocalDateTime.now().format(ARCHIVE_STAMP);
        archiveIfExists(dir.resolve(HISTORY_FILE), dir, stamp);
        archiveIfExists(dir.resolve(SUMMARY_FILE), dir, stamp);
        archiveIfExists(dir.resolve(ACT_FILE), dir, stamp);
    }

    /**
     * Appends to full-history.md — never truncated, unlike history.md which is rewritten short
     * after every compaction. This is the only place the raw wording of folded-away turns survives.
     */
    @Override
    public void archiveFoldedTurns(Path chatScenariosRoot, ChatScenario scenario, List<ChatTurn> foldedTurns) {
        if (foldedTurns.isEmpty()) return;
        Path file = chatScenariosRoot.resolve(scenario.name()).resolve(FULL_HISTORY_FILE);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, formatHistory(foldedTurns), StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException("Archivage historique complet impossible : " + file, e);
        }
    }

    @Override
    public SavePoint createSavePoint(Path chatScenariosRoot, ChatSession session) {
        String stamp = LocalDateTime.now().format(SAVE_STAMP);
        Path saveDir = chatScenariosRoot.resolve(session.scenario().name()).resolve(SAVES_DIR).resolve(stamp);
        writeSessionFiles(saveDir, session);
        return new SavePoint(stamp);
    }

    @Override
    public List<SavePoint> listSavePoints(Path chatScenariosRoot, ChatScenario scenario) {
        Path savesDir = chatScenariosRoot.resolve(scenario.name()).resolve(SAVES_DIR);
        if (!Files.isDirectory(savesDir)) return List.of();
        try (var entries = Files.list(savesDir)) {
            return entries
                .filter(Files::isDirectory)
                .map(dir -> dir.getFileName().toString())
                .sorted(Comparator.reverseOrder())
                .map(SavePoint::new)
                .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public ChatSession loadSavePoint(Path chatScenariosRoot, ChatScenario scenario, String saveId) {
        return readSessionFiles(saveDir(chatScenariosRoot, scenario, saveId), scenario);
    }

    @Override
    public void deleteSavePoint(Path chatScenariosRoot, ChatScenario scenario, String saveId) {
        Path dir = saveDir(chatScenariosRoot, scenario, saveId);
        if (!Files.isDirectory(dir)) return;
        // Toujours a plat (history.md/summary.md/act.txt, jamais de sous-dossier) : pas besoin
        // d'une suppression recursive arborescente.
        try (var entries = Files.list(dir)) {
            for (Path file : (Iterable<Path>) entries::iterator) {
                Files.delete(file);
            }
            Files.delete(dir);
        } catch (IOException e) {
            throw new UncheckedIOException("Suppression impossible : " + dir, e);
        }
    }

    private static Path saveDir(Path chatScenariosRoot, ChatScenario scenario, String saveId) {
        if (!SAVE_ID_PATTERN.matcher(saveId).matches()) {
            throw new IllegalArgumentException("Identifiant de sauvegarde invalide : " + saveId);
        }
        return chatScenariosRoot.resolve(scenario.name()).resolve(SAVES_DIR).resolve(saveId);
    }

    private static void archiveIfExists(Path file, Path dir, String stamp) {
        if (!Files.isRegularFile(file)) return;
        try {
            Path archiveDir = dir.resolve(ARCHIVE_DIR);
            Files.createDirectories(archiveDir);
            Files.move(file, archiveDir.resolve(stamp + "-" + file.getFileName()));
        } catch (IOException e) {
            throw new UncheckedIOException("Archivage impossible : " + file, e);
        }
    }

    private static String formatHistory(List<ChatTurn> turns) {
        StringBuilder sb = new StringBuilder();
        for (ChatTurn turn : turns) {
            sb.append(headerFor(turn.speaker())).append('\n').append(turn.text()).append("\n\n");
        }
        return sb.toString();
    }

    private static String headerFor(ChatTurn.Speaker speaker) {
        return switch (speaker) {
            case PLAYER   -> PLAYER_HEADER;
            case LLM      -> LLM_HEADER;
            case NARRATOR -> NARRATOR_HEADER;
        };
    }

    private static List<ChatTurn> parseHistory(String content) {
        List<ChatTurn> turns = new ArrayList<>();
        ChatTurn.Speaker current = null;
        StringBuilder buf = new StringBuilder();
        for (String line : content.split("\n", -1)) {
            if (line.equals(PLAYER_HEADER) || line.equals(LLM_HEADER) || line.equals(NARRATOR_HEADER)) {
                flush(turns, current, buf);
                current = line.equals(PLAYER_HEADER) ? ChatTurn.Speaker.PLAYER
                         : line.equals(LLM_HEADER)    ? ChatTurn.Speaker.LLM
                                                       : ChatTurn.Speaker.NARRATOR;
                buf.setLength(0);
            } else {
                if (!buf.isEmpty()) buf.append('\n');
                buf.append(line);
            }
        }
        flush(turns, current, buf);
        return turns;
    }

    private static void flush(List<ChatTurn> turns, ChatTurn.Speaker speaker, StringBuilder buf) {
        if (speaker == null) return;
        String text = buf.toString().strip();
        if (!text.isEmpty()) turns.add(new ChatTurn(speaker, text));
    }

    private static boolean existsAndReadable(Path file) {
        return Files.isRegularFile(file);
    }

    private static String readText(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Lecture impossible : " + file, e);
        }
    }

    private static void writeText(Path file, String content) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Écriture impossible : " + file, e);
        }
    }
}
