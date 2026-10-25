package storymagine.chat.infra;

import storymagine.chat.coeur.domaine.pick.PickResolver;
import storymagine.chat.coeur.domaine.scenario.Cast;
import storymagine.chat.coeur.domaine.scenario.ChatScenario;
import storymagine.chat.coeur.domaine.scenario.Npc;
import storymagine.chat.coeur.domaine.session.ChatSession;
import storymagine.chat.coeur.domaine.session.ChatTurn;
import storymagine.chat.coeur.domaine.session.Compaction;
import storymagine.chat.coeur.domaine.session.NumberedTurn;
import storymagine.chat.coeur.domaine.session.SavePoint;
import storymagine.chat.coeur.domaine.session.TurnNumber;
import storymagine.chat.coeur.ports.ChatStoragePort;
import storymagine.chat.coeur.ports.RandomnessPort;
import storymagine.chat.infra.RunLayout.SequenceFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * File-based ChatStoragePort. The scenario itself, written by hand :
 * chatScenariosRoot/{name}/scenario.txt (an optional "Joueur : X" first line — see
 * ChatScenario.playerName, stripped before the rest is parsed — then the premise, optionally
 * preceded by a "#SCENARIO" heading, followed by a nested Markdown outline of acts — see
 * ScenarioOutlineParser), optionally continued by scenario-1.txt, scenario-2.txt, ... (consecutive,
 * stopping at the first missing number, each appended verbatim after scenario.txt's own text — see
 * loadContinuationFiles), plus one ".txt" file per Cast member in characters/ (first line a
 * "# Name" heading — see loadCast). Everything the app generates while playing lives under
 * {name}/run/ and is never part of the scenario : run/current/turns/ (one file per turn, one per
 * compaction — see RunLayout, TurnFileCodec), run/current/state.txt (see RunState), run/saves/ and
 * run/archive/ (zips of current/, see RunArchiver). A scenario using [PICK: name] tags (paragraphs
 * drawn from picks/name.txt) is read from run/current/resolved/ once drawn — see scenarioTextDir.
 * A scenario directory still in the old layout is
 * refused with a message pointing to migration.bat (see LegacyLayout). See specs/chat.md, "Le run".
 */
public class ChatFileStorageAdapter implements ChatStoragePort {

    private static final String SCENARIO_FILE   = "scenario.txt";
    private static final String CHARACTERS_DIR  = "characters";
    private static final String TXT_EXTENSION   = ".txt";
    private static final String ZIP_EXTENSION   = ".zip";

    /** scenario-1.txt, scenario-2.txt, ... — see loadContinuationFiles. */
    private static final Pattern SCENARIO_CONTINUATION_FILE = Pattern.compile("scenario-(\\d+)\\.txt");
    /** First line of a continuation file must open a NEW top-level "#" heading, never "##" or deeper
     *  — see ScenarioOutlineParser.HEADING ; a continuation is only ever appended between two acts,
     *  never mid-act, so its own first heading must be one ScenarioOutlineParser treats as a fresh
     *  root, not a child folded under whatever heading scenario.txt (or the previous continuation)
     *  happened to end on. */
    private static final Pattern TOP_LEVEL_HEADING = Pattern.compile("^#(?:[^#]|$).*", Pattern.DOTALL);

    private static final String NAME_HEADING_PREFIX = "# ";
    private static final String SECRET_MARKER_LINE  = "# SECRET";
    /** Optional "Joueur : X" as the very first line of scenario.txt — see ChatScenario.playerName. */
    private static final Pattern PLAYER_NAME_LINE = Pattern.compile("^Joueur\\s*:\\s*(.+)$", Pattern.CASE_INSENSITIVE);

    /** Millisecond precision : two save points or archives made in quick succession must never share a name. */
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss-SSS");
    private static final Pattern SAVE_ID_PATTERN = Pattern.compile("[0-9-]+");

    private final RandomnessPort randomness;

    public ChatFileStorageAdapter() {
        this(new JavaRandomnessAdapter());
    }

    /** randomness draws the [PICK: ...] paragraphs — a test passes a scripted one. */
    public ChatFileStorageAdapter(RandomnessPort randomness) {
        this.randomness = randomness;
    }

    @Override
    public List<String> listScenarioNames(Path chatScenariosRoot) {
        if (!Files.isDirectory(chatScenariosRoot)) return List.of();
        try (var entries = Files.list(chatScenariosRoot)) {
            return entries
                .filter(Files::isDirectory)
                // un scenario a l'ancien format reste liste : le choisir donne le message "lancer migration.bat"
                .filter(dir -> Files.exists(dir.resolve(SCENARIO_FILE))
                    && (!loadCast(dir).npcs().isEmpty() || LegacyLayout.isLegacy(dir)))
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
        if (LegacyLayout.isLegacy(dir)) {
            throw new IllegalStateException("Ancien format de scenario detecte dans " + dir
                + " (fichiers de session ou fiches personnage a la racine) : lancer migration.bat.");
        }
        Cast cast = loadCast(dir);
        Path textDir = scenarioTextDir(chatScenariosRoot, name, dir);
        String raw = readText(textDir.resolve(SCENARIO_FILE));
        String[] lines = raw.split("\n", 2);
        Matcher m = PLAYER_NAME_LINE.matcher(lines[0].strip());
        String playerName = m.matches() ? m.group(1).strip() : null;
        String rest = m.matches() ? (lines.length > 1 ? lines[1] : "") : raw;
        String combined = appendContinuationFiles(textDir, rest);
        ScenarioOutlineParser.Outline outline = ScenarioOutlineParser.parse(combined);
        return new ChatScenario(name, cast, outline.premise(), outline.acts(), playerName, outline.premiseOpening());
    }

    /**
     * The directory scenario.txt and its continuations are read from. A scenario without PICK tags
     * is read in place. One with tags is resolved ONCE per run (see PickResolver) : the first load
     * of a run draws the paragraphs and writes the resolved files to run/current/resolved/, every
     * later load of that run reads them back, so the draws never change while the run lives. A
     * restart or a reset deletes current/, hence the next load draws again ; a save point carries
     * the resolved files along. Edits to scenario*.txt are therefore not seen until the next run.
     */
    private Path scenarioTextDir(Path chatScenariosRoot, String name, Path scenarioDir) {
        Path resolvedDir = RunLayout.resolvedDir(chatScenariosRoot, name);
        if (Files.isRegularFile(resolvedDir.resolve(SCENARIO_FILE))) return resolvedDir;

        List<Path> sources = scenarioTextFiles(scenarioDir);
        List<String> texts = sources.stream().map(ChatFileStorageAdapter::readText).toList();
        if (texts.stream().noneMatch(PickResolver::containsTag)) return scenarioDir;

        List<String> resolved = new PickResolver(new PickFileLibrary(scenarioDir), randomness).resolveAll(texts);
        for (int i = 0; i < sources.size(); i++) {
            writeAtomically(resolvedDir.resolve(sources.get(i).getFileName()), resolved.get(i));
        }
        return resolvedDir;
    }

    /** scenario.txt, then scenario-1.txt, scenario-2.txt, ... — consecutive numbers, stopping at the first missing one. */
    private static List<Path> scenarioTextFiles(Path dir) {
        List<Path> files = new ArrayList<>(List.of(dir.resolve(SCENARIO_FILE)));
        for (int n = 1; Files.isRegularFile(dir.resolve("scenario-" + n + TXT_EXTENSION)); n++) {
            files.add(dir.resolve("scenario-" + n + TXT_EXTENSION));
        }
        return files;
    }

    /**
     * Appends scenario-1.txt, scenario-2.txt, ... after rest (scenario.txt's own text, Joueur line
     * already stripped) — consecutive numbers only, stopping at the first missing one. Each file's
     * content is required to open on a fresh top-level "#" heading (see TOP_LEVEL_HEADING) : that's
     * what lets a plain string concatenation be handed to ScenarioOutlineParser unchanged, instead
     * of risking a continuation's first lines silently getting folded into the previous file's last
     * heading as body text.
     */
    private static String appendContinuationFiles(Path dir, String rest) {
        StringBuilder combined = new StringBuilder(rest);
        for (int n = 1; ; n++) {
            Path file = dir.resolve("scenario-" + n + TXT_EXTENSION);
            if (!Files.isRegularFile(file)) break;
            String continuation = readText(file);
            if (!TOP_LEVEL_HEADING.matcher(firstLine(continuation)).matches()) {
                throw new IllegalStateException(
                    file.getFileName() + " doit commencer par un titre \"# ...\" de premier niveau, pas \"##\" ou du texte libre : " + file);
            }
            combined.append("\n\n").append(continuation);
        }
        return combined.toString();
    }

    /**
     * Every ".txt" file of the scenario's characters/ directory whose first line is a "# Name"
     * heading, alphabetical by filename. Only characters/ is read, so a stray file at the scenario
     * root can never become a Cast member ; the heading requirement still quietly ignores a draft or
     * a note dropped into characters/ instead of turning it into a nameless Npc.
     */
    private static Cast loadCast(Path scenarioDir) {
        Path dir = scenarioDir.resolve(CHARACTERS_DIR);
        if (!Files.isDirectory(dir)) return new Cast(List.of());
        try (var entries = Files.list(dir)) {
            List<Npc> npcs = entries
                .filter(Files::isRegularFile)
                .filter(f -> f.getFileName().toString().endsWith(TXT_EXTENSION))
                .filter(ChatFileStorageAdapter::hasNameHeading)
                .sorted(Comparator.comparing(f -> f.getFileName().toString()))
                .map(ChatFileStorageAdapter::parseNpcFile)
                .toList();
            return new Cast(npcs);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static boolean hasNameHeading(Path file) {
        return firstLine(readText(file)).startsWith(NAME_HEADING_PREFIX);
    }

    private static String firstLine(String content) {
        int newline = content.indexOf('\n');
        return (newline < 0 ? content : content.substring(0, newline)).strip();
    }

    /**
     * id = filename without ".txt". name = optional "# Name" first line (kept verbatim in
     * publicInfo, not stripped — the character should still read it there). An optional "# SECRET"
     * line (on its own, case-insensitive) splits the rest : everything before is publicInfo,
     * everything after is secretInfo — see Npc. No such line : the whole file is publicInfo,
     * secretInfo is "" (a purely public character).
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

    private static String extractCharacterName(String content) {
        String firstLine = firstLine(content);
        return firstLine.startsWith(NAME_HEADING_PREFIX) ? firstLine.substring(NAME_HEADING_PREFIX.length()).strip() : "";
    }

    // ── Le run : etat courant ───────────────────────────────────────────────

    @Override
    public ChatSession loadSession(Path chatScenariosRoot, ChatScenario scenario) {
        return readCurrentRun(chatScenariosRoot, scenario);
    }

    @Override
    public boolean hasPlayedTurns(Path chatScenariosRoot, ChatScenario scenario) {
        for (SequenceFile file : RunLayout.listSequence(RunLayout.turnsDir(chatScenariosRoot, scenario.name()))) {
            if (file.kind() != SequenceFile.Kind.TURN) continue;
            if (TurnFileCodec.parse(readText(file.path()), file.path()).turn().speaker() != ChatTurn.Speaker.NARRATOR) return true;
        }
        return false;
    }

    /**
     * Rebuilds the current run just after its last persisted turn. Neither a turn file nor a
     * state.txt means the run was never written (a scenario played for the very first time,
     * "continuer" chosen out of habit on something brand new) : a hollow ChatSession here would
     * silently drop the opening act's "[...]" beats, so this delegates to fresh() for the same
     * opening every other reset path gets. The act comes from state.txt when present — it can
     * differ from the last turn's act after a manual "previous act".
     */
    private static ChatSession readCurrentRun(Path root, ChatScenario scenario) {
        Path turnsDir = RunLayout.turnsDir(root, scenario.name());
        Path stateFile = RunLayout.stateFile(root, scenario.name());
        int last = lastTurnNumber(RunLayout.listSequence(turnsDir));
        if (last == 0 && !Files.isRegularFile(stateFile)) return ChatSession.fresh(scenario);
        ChatSession atLast = rebuildAt(root, scenario, last);
        RunState state = readState(stateFile);
        if (state.act() == null) return atLast;
        return ChatSession.restored(scenario, atLast.summary(), atLast.firstTurnNumber(), atLast.numberedTurns(),
            state.act(), atLast.presentNpcIds(), atLast.interjectingNpcIds());
    }

    @Override
    public ChatSession loadSessionAt(Path chatScenariosRoot, ChatScenario scenario, TurnNumber at) {
        int last = lastTurnNumber(RunLayout.listSequence(RunLayout.turnsDir(chatScenariosRoot, scenario.name())));
        if (at.value() > last) {
            throw new IllegalArgumentException("Tour #" + at.value() + " inexistant : le dernier tour est #" + last);
        }
        return rebuildAt(chatScenariosRoot, scenario, at.value());
    }

    /**
     * The state just after turn n (specs/chat.md, algorithm C) : the most recent compaction made at
     * or before n gives the summary and where the verbatim window starts (foldedUpTo + 1, or turn 1
     * without any compaction) ; only the turn files of that window are read. The act is the one
     * written on turn n itself ; presence/interjection come from state.txt, defaulting to everyone.
     */
    private static ChatSession rebuildAt(Path root, ChatScenario scenario, int n) {
        Path turnsDir = RunLayout.turnsDir(root, scenario.name());
        SequenceFile compaction = RunLayout.listSequence(turnsDir).stream()
            .filter(f -> f.kind() == SequenceFile.Kind.COMPACTION && f.number() <= n)
            .reduce((first, second) -> second)
            .orElse(null);
        String summary = compaction == null ? "" : readText(compaction.path());
        int windowStart = compaction == null ? 1 : compaction.foldedUpTo() + 1;

        List<NumberedTurn> window = new ArrayList<>();
        for (int i = windowStart; i <= n; i++) window.add(readTurn(turnsDir, i));
        int act = n == 0
            ? (scenario.acts().isEmpty() ? 0 : 1)
            : (window.isEmpty() ? readTurn(turnsDir, n) : window.get(window.size() - 1)).act();

        RunState state = readState(RunLayout.stateFile(root, scenario.name()));
        Set<String> present = state.present() != null ? state.present() : ChatSession.allPresent(scenario);
        Set<String> interjecting = state.interjecting() != null ? state.interjecting() : ChatSession.allPresent(scenario);
        return ChatSession.restored(scenario, summary, TurnNumber.of(windowStart), window, act, present, interjecting);
    }

    private static NumberedTurn readTurn(Path turnsDir, int number) {
        Path file = turnsDir.resolve(RunLayout.turnFileName(number));
        if (!Files.isRegularFile(file)) {
            throw new IllegalStateException("Fichier de tour manquant : " + file);
        }
        NumberedTurn turn = TurnFileCodec.parse(readText(file), file);
        if (turn.number().value() != number) {
            throw new IllegalStateException("Le fichier " + file.getFileName() + " contient le tour #" + turn.number().value());
        }
        return turn;
    }

    private static int lastTurnNumber(List<SequenceFile> sequence) {
        return sequence.stream()
            .filter(f -> f.kind() == SequenceFile.Kind.TURN)
            .mapToInt(SequenceFile::number)
            .max()
            .orElse(0);
    }

    private static RunState readState(Path stateFile) {
        return Files.isRegularFile(stateFile) ? RunState.parse(readText(stateFile)) : new RunState(null, null, null);
    }

    @Override
    public void saveSession(Path chatScenariosRoot, ChatSession session) {
        String name = session.scenario().name();
        Path turnsDir = RunLayout.turnsDir(chatScenariosRoot, name);
        createDirectories(turnsDir);
        int last = session.lastTurnNumber().value();
        for (SequenceFile file : RunLayout.listSequence(turnsDir)) {
            if (file.number() > last) deleteFile(file.path());
        }
        for (NumberedTurn numbered : session.numberedTurns()) {
            writeIfChanged(turnsDir.resolve(RunLayout.turnFileName(numbered.number().value())),
                TurnFileCodec.format(numbered));
        }
        writeAtomically(RunLayout.stateFile(chatScenariosRoot, name),
            new RunState(session.currentAct(), session.presentNpcIds(), session.interjectingNpcIds()).format());
    }

    @Override
    public void writeCompaction(Path chatScenariosRoot, ChatScenario scenario, Compaction compaction) {
        Path turnsDir = RunLayout.turnsDir(chatScenariosRoot, scenario.name());
        createDirectories(turnsDir);
        int createdAfter = compaction.createdAfter().value();
        // createdAfter est unique dans une histoire lineaire : une ancienne compaction au meme
        // endroit (autre foldedUpTo) est remplacee, jamais gardee a cote.
        for (SequenceFile file : RunLayout.listSequence(turnsDir)) {
            if (file.kind() == SequenceFile.Kind.COMPACTION && file.number() == createdAfter) deleteFile(file.path());
        }
        writeAtomically(turnsDir.resolve(RunLayout.compactionFileName(createdAfter, compaction.foldedUpTo().value())),
            compaction.summary());
    }

    @Override
    public void truncateAfter(Path chatScenariosRoot, ChatScenario scenario, TurnNumber after) {
        for (SequenceFile file : RunLayout.listSequence(RunLayout.turnsDir(chatScenariosRoot, scenario.name()))) {
            if (file.number() > after.value()) deleteFile(file.path());
        }
    }

    /** Archives (never deletes) the current run on reset — a zip of current/, then current/ is cleared. */
    @Override
    public void resetSession(Path chatScenariosRoot, ChatScenario scenario) {
        Path current = RunLayout.currentDir(chatScenariosRoot, scenario.name());
        if (!Files.isDirectory(current)) return;
        RunArchiver.zip(current, RunLayout.archiveDir(chatScenariosRoot, scenario.name())
            .resolve(LocalDateTime.now().format(STAMP) + ZIP_EXTENSION));
        RunArchiver.deleteRecursively(current);
    }

    // ── Sauvegardes manuelles ───────────────────────────────────────────────

    @Override
    public SavePoint createSavePoint(Path chatScenariosRoot, ChatSession session) {
        String name = session.scenario().name();
        String stamp = LocalDateTime.now().format(STAMP);
        RunArchiver.zip(RunLayout.currentDir(chatScenariosRoot, name),
            RunLayout.savesDir(chatScenariosRoot, name).resolve(stamp + ZIP_EXTENSION));
        return new SavePoint(stamp, session.lastTurnNumber().value());
    }

    @Override
    public List<SavePoint> listSavePoints(Path chatScenariosRoot, ChatScenario scenario) {
        Path savesDir = RunLayout.savesDir(chatScenariosRoot, scenario.name());
        if (!Files.isDirectory(savesDir)) return List.of();
        try (var entries = Files.list(savesDir)) {
            return entries
                .filter(Files::isRegularFile)
                .filter(f -> f.getFileName().toString().endsWith(ZIP_EXTENSION))
                .sorted(Comparator.comparing((Path f) -> f.getFileName().toString()).reversed())
                .map(f -> new SavePoint(saveIdOf(f), RunArchiver.lastTurnNumber(f)))
                .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public ChatSession loadSavePoint(Path chatScenariosRoot, ChatScenario scenario, String saveId) {
        Path zip = saveZip(chatScenariosRoot, scenario, saveId);
        if (!Files.isRegularFile(zip)) {
            throw new IllegalArgumentException("Sauvegarde inconnue : " + saveId);
        }
        RunArchiver.extractInto(zip, RunLayout.currentDir(chatScenariosRoot, scenario.name()));
        // The save carries its own resolved scenario (PICK draws) : the scenario is read again from it.
        return readCurrentRun(chatScenariosRoot, loadScenario(chatScenariosRoot, scenario.name()));
    }

    @Override
    public void deleteSavePoint(Path chatScenariosRoot, ChatScenario scenario, String saveId) {
        deleteFile(saveZip(chatScenariosRoot, scenario, saveId));
    }

    private static Path saveZip(Path chatScenariosRoot, ChatScenario scenario, String saveId) {
        if (!SAVE_ID_PATTERN.matcher(saveId).matches()) {
            throw new IllegalArgumentException("Identifiant de sauvegarde invalide : " + saveId);
        }
        return RunLayout.savesDir(chatScenariosRoot, scenario.name()).resolve(saveId + ZIP_EXTENSION);
    }

    private static String saveIdOf(Path zip) {
        String fileName = zip.getFileName().toString();
        return fileName.substring(0, fileName.length() - ZIP_EXTENSION.length());
    }

    // ── Fichiers ────────────────────────────────────────────────────────────

    private static String readText(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Lecture impossible : " + file, e);
        }
    }

    private static void createDirectories(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new UncheckedIOException("Creation impossible : " + dir, e);
        }
    }

    private static void deleteFile(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new UncheckedIOException("Suppression impossible : " + file, e);
        }
    }

    /** A turn is rewritten only if its file is missing or different — a window of a few dozen turns is re-checked after every turn. */
    private static void writeIfChanged(Path file, String content) {
        if (Files.isRegularFile(file) && readText(file).equals(content)) return;
        writeAtomically(file, content);
    }

    /** Written under a temporary name then moved into place : a crash never leaves a half-written file. */
    private static void writeAtomically(Path file, String content) {
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, content, StandardCharsets.UTF_8);
            try {
                Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Écriture impossible : " + file, e);
        }
    }
}
