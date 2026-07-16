package storymagine.chat.infra;

import storymagine.chat.coeur.domaine.scenario.Cast;
import storymagine.chat.coeur.domaine.scenario.ChatScenario;
import storymagine.chat.coeur.domaine.scenario.Npc;
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
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * File-based ChatStoragePort : chatScenariosRoot/{name}/scenario.txt (single file : an optional
 * "Joueur : X" first line — see ChatScenario.playerName, stripped before the rest is parsed —
 * then the premise, optionally preceded by a "#SCENARIO" heading, followed by a nested Markdown
 * outline of acts — see ScenarioOutlineParser) + one ".txt" file per Cast member (any name except scenario.txt,
 * act.txt, present.txt, interject.txt — see loadCast ; "character.txt" is just an ordinary Npc
 * file under this rule, which is how a legacy single-character scenario naturally becomes a
 * one-Npc Cast with no special case) and history.md + summary.md + act.txt + present.txt +
 * interject.txt (persisted session, rewritten after every turn and on reset) + full-history.md
 * (append-only archive of every turn ever folded into the summary — never rewritten, never
 * truncated). saves/{stamp}/ holds the same five session files per player-triggered save point
 * (see createSavePoint), never touched by the automatic save/reset flow above. See ChatStoragePort
 * for the directory convention.
 */
public class ChatFileStorageAdapter implements ChatStoragePort {

    private static final String SCENARIO_FILE  = "scenario.txt";
    private static final String HISTORY_FILE      = "history.md";
    private static final String SUMMARY_FILE      = "summary.md";
    private static final String ACT_FILE          = "act.txt";
    private static final String PRESENT_FILE      = "present.txt";
    private static final String INTERJECT_FILE    = "interject.txt";
    private static final String FULL_HISTORY_FILE = "full-history.md";
    private static final String TXT_EXTENSION = ".txt";

    /** Top-level ".txt" files that are session/scenario plumbing, never a Cast member's sheet. */
    private static final Set<String> RESERVED_TXT_FILES = Set.of(SCENARIO_FILE, ACT_FILE, PRESENT_FILE, INTERJECT_FILE);

    private static final String NAME_HEADING_PREFIX = "# ";
    private static final String SECRET_MARKER_LINE  = "# SECRET";
    /** Optional "Joueur : X" as the very first line of scenario.txt — see ChatScenario.playerName. */
    private static final Pattern PLAYER_NAME_LINE = Pattern.compile("^Joueur\\s*:\\s*(.+)$", Pattern.CASE_INSENSITIVE);

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
                .filter(dir -> Files.exists(dir.resolve(SCENARIO_FILE)) && !loadCast(dir).npcs().isEmpty())
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
        Cast cast = loadCast(dir);
        String raw = readText(dir.resolve(SCENARIO_FILE));
        String[] lines = raw.split("\n", 2);
        Matcher m = PLAYER_NAME_LINE.matcher(lines[0].strip());
        String playerName = m.matches() ? m.group(1).strip() : null;
        String rest = m.matches() ? (lines.length > 1 ? lines[1] : "") : raw;
        ScenarioOutlineParser.Outline outline = ScenarioOutlineParser.parse(rest);
        return new ChatScenario(name, cast, outline.premise(), outline.acts(), playerName);
    }

    /** Every ".txt" file in dir except the reserved ones (see RESERVED_TXT_FILES), alphabetical by filename. */
    private static Cast loadCast(Path dir) {
        if (!Files.isDirectory(dir)) return new Cast(List.of());
        try (var entries = Files.list(dir)) {
            List<Npc> npcs = entries
                .filter(Files::isRegularFile)
                .filter(f -> f.getFileName().toString().endsWith(TXT_EXTENSION))
                .filter(f -> !RESERVED_TXT_FILES.contains(f.getFileName().toString()))
                .sorted(Comparator.comparing(f -> f.getFileName().toString()))
                .map(ChatFileStorageAdapter::parseNpcFile)
                .toList();
            return new Cast(npcs);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * id = filename without ".txt". name = optional "# Name" first line (kept verbatim in
     * publicInfo, not stripped — the character should still read it there, same convention as the
     * old single-character.txt). An optional "# SECRET" line (on its own, case-insensitive) splits
     * the rest : everything before is publicInfo, everything after is secretInfo — see Npc. No
     * such line : the whole file is publicInfo, secretInfo is "" (a purely public character).
     */
    private static Npc parseNpcFile(Path file) {
        String fileName = file.getFileName().toString();
        String id = fileName.substring(0, fileName.length() - TXT_EXTENSION.length());
        String content = readText(file);
        String name = extractCharacterName(content);

        String[] lines = content.split("\n", -1);
        int secretLineIdx = -1;
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].strip().equalsIgnoreCase(SECRET_MARKER_LINE)) { secretLineIdx = i; break; }
        }
        String publicInfo;
        String secretInfo;
        if (secretLineIdx < 0) {
            publicInfo = content.strip();
            secretInfo = "";
        } else {
            publicInfo = String.join("\n", Arrays.copyOfRange(lines, 0, secretLineIdx)).strip();
            secretInfo = String.join("\n", Arrays.copyOfRange(lines, secretLineIdx + 1, lines.length)).strip();
        }
        return new Npc(id, name, publicInfo, secretInfo);
    }

    /** Optional "# Name" first line of an Npc file — kept in publicInfo, not stripped. */
    private static String extractCharacterName(String content) {
        int newline = content.indexOf('\n');
        String firstLine = (newline < 0 ? content : content.substring(0, newline)).strip();
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

    /**
     * Shared by loadSession and loadSavePoint — same five files, different directory. No
     * history.md at all means this directory was never written by writeSessionFiles (a scenario
     * played for the very first time, "continuer" chosen out of habit on something brand new) —
     * building a hollow ChatSession here used to silently drop the opening act's "[...]" beats
     * (only ChatSession.fresh() produced them), leaving the player with no introduction at all.
     * Delegating to fresh() instead gives the same opening every other "reset" path already gets.
     */
    private static ChatSession readSessionFiles(Path dir, ChatScenario scenario) {
        if (!existsAndReadable(dir.resolve(HISTORY_FILE))) {
            return ChatSession.fresh(scenario);
        }
        String summary = existsAndReadable(dir.resolve(SUMMARY_FILE)) ? readText(dir.resolve(SUMMARY_FILE)) : "";
        List<ChatTurn> turns = existsAndReadable(dir.resolve(HISTORY_FILE))
            ? parseHistory(readText(dir.resolve(HISTORY_FILE)))
            : List.of();
        int currentAct = existsAndReadable(dir.resolve(ACT_FILE))
            ? Integer.parseInt(readText(dir.resolve(ACT_FILE)).strip())
            : (scenario.acts().isEmpty() ? 0 : 1);
        Set<String> presentNpcIds = existsAndReadable(dir.resolve(PRESENT_FILE))
            ? parseIdList(readText(dir.resolve(PRESENT_FILE)))
            : ChatSession.allPresent(scenario);
        Set<String> interjectingNpcIds = existsAndReadable(dir.resolve(INTERJECT_FILE))
            ? parseIdList(readText(dir.resolve(INTERJECT_FILE)))
            : ChatSession.allPresent(scenario); // meme defaut "tout le monde" qu'une session neuve
        return new ChatSession(scenario, turns, summary, currentAct, presentNpcIds, interjectingNpcIds);
    }

    private static Set<String> parseIdList(String content) {
        Set<String> ids = new LinkedHashSet<>();
        for (String line : content.split("\n")) {
            String id = line.strip();
            if (!id.isEmpty()) ids.add(id);
        }
        return ids;
    }

    /** Shared by saveSession and createSavePoint — same five files, different directory. */
    private static void writeSessionFiles(Path dir, ChatSession session) {
        writeText(dir.resolve(HISTORY_FILE), formatHistory(session.turns()));
        writeText(dir.resolve(SUMMARY_FILE), session.summary());
        writeText(dir.resolve(ACT_FILE), String.valueOf(session.currentAct()));
        writeText(dir.resolve(PRESENT_FILE), String.join("\n", session.presentNpcIds()));
        writeText(dir.resolve(INTERJECT_FILE), String.join("\n", session.interjectingNpcIds()));
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
        archiveIfExists(dir.resolve(PRESENT_FILE), dir, stamp);
        archiveIfExists(dir.resolve(INTERJECT_FILE), dir, stamp);
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
        // Toujours a plat (history.md/summary.md/act.txt/present.txt, jamais de sous-dossier) :
        // pas besoin d'une suppression recursive arborescente.
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
            sb.append(headerFor(turn)).append('\n').append(turn.text()).append("\n\n");
        }
        return sb.toString();
    }

    /** LLM turns carry their npcId as "### LLM: {id}" ; PLAYER/NARRATOR never have one to carry. */
    private static String headerFor(ChatTurn turn) {
        return switch (turn.speaker()) {
            case PLAYER   -> PLAYER_HEADER;
            case LLM      -> turn.npcId() != null ? LLM_HEADER + ": " + turn.npcId() : LLM_HEADER;
            case NARRATOR -> NARRATOR_HEADER;
        };
    }

    private static List<ChatTurn> parseHistory(String content) {
        List<ChatTurn> turns = new ArrayList<>();
        ChatTurn.Speaker current = null;
        String currentNpcId = null;
        StringBuilder buf = new StringBuilder();
        for (String line : content.split("\n", -1)) {
            if (line.equals(PLAYER_HEADER) || line.equals(NARRATOR_HEADER) || isLlmHeader(line)) {
                flush(turns, current, currentNpcId, buf);
                if (line.equals(PLAYER_HEADER))        { current = ChatTurn.Speaker.PLAYER;   currentNpcId = null; }
                else if (line.equals(NARRATOR_HEADER)) { current = ChatTurn.Speaker.NARRATOR;  currentNpcId = null; }
                else                                    { current = ChatTurn.Speaker.LLM;       currentNpcId = llmHeaderNpcId(line); }
                buf.setLength(0);
            } else {
                if (!buf.isEmpty()) buf.append('\n');
                buf.append(line);
            }
        }
        flush(turns, current, currentNpcId, buf);
        return turns;
    }

    private static boolean isLlmHeader(String line) {
        return line.equals(LLM_HEADER) || line.startsWith(LLM_HEADER + ": ");
    }

    /** null for a bare "### LLM" (pre-multi-NPC history — see ChatTurn.npcId). */
    private static String llmHeaderNpcId(String line) {
        return line.equals(LLM_HEADER) ? null : line.substring((LLM_HEADER + ": ").length());
    }

    private static void flush(List<ChatTurn> turns, ChatTurn.Speaker speaker, String npcId, StringBuilder buf) {
        if (speaker == null) return;
        String text = buf.toString().strip();
        if (!text.isEmpty()) turns.add(new ChatTurn(speaker, text, "", npcId));
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
