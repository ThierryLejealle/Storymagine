package storymagine.chat.coeur.domaine.validation;

import storymagine.chat.coeur.domaine.activity.ActivityDirective;
import storymagine.chat.coeur.domaine.activity.ActivityDirectiveMarker;
import storymagine.chat.coeur.domaine.activity.ActivityResolver;
import storymagine.chat.coeur.domaine.pick.CastCommand;
import storymagine.chat.coeur.domaine.pick.CastPool;
import storymagine.chat.coeur.domaine.pick.PickLibrary;
import storymagine.chat.coeur.domaine.pick.PickResolver;
import storymagine.chat.coeur.domaine.pick.RepeatExpander;
import storymagine.chat.coeur.domaine.scenario.Cast;
import storymagine.chat.coeur.domaine.scenario.CharacterDelta;
import storymagine.chat.coeur.domaine.scenario.CharacterDeltaMarker;
import storymagine.chat.coeur.domaine.scenario.ChatScenario;
import storymagine.chat.coeur.domaine.scenario.LegacyCommandSyntax;
import storymagine.chat.coeur.domaine.scenario.NextActMarker;
import storymagine.chat.coeur.domaine.scenario.Npc;
import storymagine.chat.coeur.domaine.scenario.NpcOnlyMarker;
import storymagine.chat.coeur.domaine.scenario.NpcOpening;
import storymagine.chat.coeur.domaine.scenario.NpcOpeningMarker;
import storymagine.chat.coeur.domaine.scenario.PrivateLines;
import storymagine.chat.coeur.domaine.scenario.ScenarioAct;
import storymagine.chat.coeur.domaine.scenario.SceneStyleMarker;
import storymagine.chat.coeur.domaine.scenario.ScenarioStyle;
import storymagine.chat.coeur.domaine.scenario.StartingRoster;
import storymagine.chat.coeur.domaine.scenario.StoryDirective;
import storymagine.chat.coeur.domaine.scenario.StoryDirectiveMarker;
import storymagine.chat.coeur.domaine.scenario.StoryTag;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/**
 * Static checks of a scenario, no LLM involved. checkSources reads what the author wrote, line by
 * line, and collects EVERY problem (old bracket commands, unknown or malformed {commands}, a pick
 * library that is missing or too short, a character that does not exist, a command that must be
 * alone on its line) so one run lists all of them instead of stopping at the first. checkScenario
 * looks at the parsed scenario for things worth a warning (an act without a "[NEXT ACT]" condition,
 * a character the first act never mentions). Pure : same input, same report.
 */
public final class ScenarioValidator {

    private static final Set<String> ACTIVITY_COMMANDS = Set.of("npc-active", "npc-present", "npc-passive", "npc-absent", "npc-reset", "npc-mode",
        "npc-arrives", "npc-leaves");
    private static final Set<String> COMMANDS = Set.of("pick", "cast", "style", "npc-starts", "npc-delta", "npc-story", "npc-active", "npc-present",
        "npc-passive", "npc-absent", "npc-reset", "npc-mode", "npc-arrives", "npc-leaves", "npc-only", "npc-do", "repeat", "end-repeat", "n", "item",
        "private", "end-private");
    /** Checked as a whole by RepeatExpander (blocks, zones, piece counts), not line by line. */
    private static final Set<String> REPEAT_COMMANDS = Set.of("repeat", "end-repeat", "n", "item");
    /** Checked as a whole by PrivateLines (position, names, blocks closed), file by file : see checkPrivateLines. */
    private static final Set<String> PRIVATE_COMMANDS = Set.of("private", "end-private");
    /** A line that opens an "{npc-delta" : the command may then run over several lines, see checkDeltaBlock. */
    private static final Pattern DELTA_START = Pattern.compile("^\\s*\\{\\s*npc-delta", Pattern.CASE_INSENSITIVE);
    private static final Pattern COMMAND_START =Pattern.compile("\\{\\s*([A-Za-z][A-Za-z-]*)");
    private static final Pattern COMMAND_TEXT = Pattern.compile("\\{[^}]*}");
    /** "%NAME%" (group 1 : NAME) : a character drawn by a {cast}. */
    private static final Pattern VARIABLE_USE = Pattern.compile("%([A-Za-z][A-Za-z0-9_]*)%");
    private static final Pattern REPEAT_LINE = Pattern.compile("^\\{\\s*repeat(?![\\p{L}\\p{N}_-])([^}]*)}$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CAST_SOURCE = Pattern.compile("(?:^|\\s)cast\\s+([A-Za-z][A-Za-z0-9_]*)");

    public static final String STYLE_FILE = "style.txt";
    /** Beyond this many characters the style advice starts to weigh on a small model's context. */
    static final int STYLE_WARNING_LENGTH = 1500;

    private ScenarioValidator() {}

    /** Errors only (warnings need the parsed scenario, see checkScenario). */
    public static ScenarioReport checkSources(List<ScenarioSource> sources, Cast cast, PickLibrary library) {
        List<ScenarioIssue> issues = new ArrayList<>();
        for (ScenarioSource source : sources) {
            String[] lines = source.text().replace("\r\n", "\n").replace("\r", "\n").split("\n", -1);
            for (int i = 0; i < lines.length; i++) {
                String where = source.fileName() + ":" + (i + 1);
                if (DELTA_START.matcher(lines[i]).find()) {
                    i = checkDeltaBlock(where, lines, i, cast, issues);
                } else {
                    checkLine(where, lines[i], cast, library, issues);
                }
            }
        }
        for (ScenarioSource source : sources) checkChanceMarkers(source.fileName(), source.text(), issues);
        checkRepeatBlocks(sources, library, cast, issues);
        checkPrivateLines(sources, cast, issues);
        checkVariables(sources, issues);
        if (issues.isEmpty()) checkUniqueDoesNotRunOut(sources, library, cast, issues);
        return new ScenarioReport(issues);
    }

    /** Each file on its own, to name the file : blocks closed, zones inside a block, library long enough, pieces count right. */
    private static void checkRepeatBlocks(List<ScenarioSource> sources, PickLibrary library, Cast cast, List<ScenarioIssue> issues) {
        for (ScenarioSource source : sources) {
            try {
                new RepeatExpander(library, bound -> 0, cast).expand(List.of(source.text()));
            } catch (IllegalArgumentException | IllegalStateException e) {
                issues.add(ScenarioIssue.error(source.fileName(), e.getMessage()));
            }
        }
    }

    /** Each file on its own, to name the file : "{private}" well placed, names known, blocks closed, nothing forbidden inside. */
    private static void checkPrivateLines(List<ScenarioSource> sources, Cast cast, List<ScenarioIssue> issues) {
        for (ScenarioSource source : sources) {
            try {
                PrivateLines.validate(source.text().replace("\r\n", "\n").replace("\r", "\n"), cast);
            } catch (IllegalArgumentException e) {
                issues.add(ScenarioIssue.error(source.fileName(), e.getMessage()));
            }
        }
    }

    private static void checkLine(String where, String line, Cast cast, PickLibrary library, List<ScenarioIssue> issues) {
        try {
            LegacyCommandSyntax.requireNone(line);
        } catch (IllegalArgumentException e) {
            issues.add(ScenarioIssue.error(where, e.getMessage()));
        }
        // A %NAME% stands for a character drawn when the game starts : the line is checked with one of the cast in its place.
        boolean usesVariable = VARIABLE_USE.matcher(line).find();
        String checked = usesVariable ? VARIABLE_USE.matcher(line).replaceAll(Matcher.quoteReplacement(standIn(cast))) : line;
        Matcher start = COMMAND_START.matcher(checked);
        while (start.find()) {
            String name = start.group(1).toLowerCase(Locale.ROOT);
            Matcher text = COMMAND_TEXT.matcher(checked.substring(start.start()));
            String command = text.find() ? text.group() : checked.substring(start.start());
            if (name.equals("delta-pnj")) {
                continue; // the old name is already reported, with the new one, by LegacyCommandSyntax
            }
            if (!COMMANDS.contains(name)) {
                issues.add(ScenarioIssue.error(where, "commande inconnue {" + name + " ...} (commandes : pick, repeat, cast, style, npc-starts, npc-do, "
                    + "npc-delta, npc-story, npc-active, npc-present, npc-passive, npc-absent, npc-reset, npc-mode, npc-arrives, npc-leaves, npc-only, private)"));
            } else if (REPEAT_COMMANDS.contains(name) || PRIVATE_COMMANDS.contains(name)) {
                continue;
            } else if (name.equals("style")) {
                try {
                    SceneStyleMarker.extract(checked);
                } catch (IllegalArgumentException e) {
                    issues.add(ScenarioIssue.error(where, e.getMessage()));
                }
            } else if (name.equals("pick")) {
                checkPick(where, command, library, issues);
            } else if (name.equals("cast")) {
                checkCast(where, checked, cast, issues);
            } else if (name.equals("npc-starts") || name.equals("npc-do")) {
                checkNpcStarts(where, checked, cast, issues, name);
            } else if (ACTIVITY_COMMANDS.contains(name)) {
                checkActivity(where, checked, cast, issues);
            } else if (name.equals("npc-only")) {
                checkNpcOnly(where, checked, cast, issues);
            } else if (name.equals("npc-story")) {
                checkStory(where, checked, cast, issues, usesVariable);
            } else {
                checkDelta(where, checked, cast, issues);
            }
        }
    }

    private static String standIn(Cast cast) {
        return cast.npcs().isEmpty() ? "x" : cast.npcs().get(0).label();
    }

    /** The command is well formed and every character it lists exists, once. Whether a combination exists is checked by the draw itself. */
    private static void checkCast(String where, String line, Cast cast, List<ScenarioIssue> issues) {
        try {
            Optional<CastCommand> command = CastCommand.parseLine(line);
            if (command.isEmpty()) return;
            try {
                CastPool.of(command.get().candidates(), cast);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(command.get().source() + " : " + e.getMessage());
            }
        } catch (IllegalArgumentException e) {
            issues.add(ScenarioIssue.error(where, e.getMessage()));
        }
    }

    /** %NAME% must be declared by a {cast NAME} (or a {repeat ... cast NAME}) above it, and is forgotten at the end of the repeat block that declared it. */
    private static void checkVariables(List<ScenarioSource> sources, List<ScenarioIssue> issues) {
        Set<String> declared = new HashSet<>();
        Set<String> beforeBlock = null;
        for (ScenarioSource source : sources) {
            String[] lines = source.text().replace("\r\n", "\n").replace("\r", "\n").split("\n", -1);
            for (int i = 0; i < lines.length; i++) {
                String where = source.fileName() + ":" + (i + 1);
                Matcher use = VARIABLE_USE.matcher(lines[i]);
                while (use.find()) {
                    if (!declared.contains(use.group(1).toLowerCase(Locale.ROOT))) {
                        issues.add(ScenarioIssue.error(where, use.group() + " n'est pas declaree avant cette ligne : ecrire {cast "
                            + use.group(1) + "} plus haut (ou {repeat ... cast " + use.group(1) + "})"));
                    }
                }
                Matcher repeat = REPEAT_LINE.matcher(lines[i].strip());
                if (repeat.matches()) {
                    beforeBlock = new HashSet<>(declared);
                    Matcher castSource = CAST_SOURCE.matcher(repeat.group(1));
                    if (castSource.find()) declared.add(castSource.group(1).toLowerCase(Locale.ROOT));
                } else if (lines[i].strip().toLowerCase(Locale.ROOT).startsWith("{end-repeat") && beforeBlock != null) {
                    declared = beforeBlock;
                    beforeBlock = null;
                } else {
                    String variable = declaredBy(lines[i]);
                    if (variable != null) declared.add(variable);
                }
            }
        }
    }

    private static void checkPick(String where, String command, PickLibrary library, List<ScenarioIssue> issues) {
        try {
            new PickResolver(library, bound -> 0).resolve(List.of(command));
        } catch (IllegalArgumentException | IllegalStateException e) {
            issues.add(ScenarioIssue.error(where, e.getMessage()));
        }
    }

    private static void checkNpcStarts(String where, String line, Cast cast, List<ScenarioIssue> issues, String command) {
        NpcOpening opening = NpcOpeningMarker.extract(line + "\n").opening();
        if (opening.isNone()) {
            issues.add(ScenarioIssue.error(where, "{" + command + "} doit etre seul sur sa ligne : {" + command + "} ou {" + command + " Nom}"));
        } else {
            try {
                ActivityResolver.openingNpcs(cast, opening);
            } catch (IllegalArgumentException e) {
                issues.add(ScenarioIssue.error(where, "{" + command + " " + opening.namesText() + "} : " + e.getMessage()));
            }
        }
    }

    private static void checkActivity(String where, String line, Cast cast, List<ScenarioIssue> issues) {
        try {
            List<ActivityDirective> directives = ActivityDirectiveMarker.extract(line + "\n").directives();
            if (directives.isEmpty()) {
                issues.add(ScenarioIssue.error(where, "une commande d'activite doit etre seule sur sa ligne : "
                    + "{npc-active|present|passive|absent noms}, {npc-reset}, {npc-mode mode on|off nom|*}, {npc-arrives noms} ou {npc-leaves noms}"));
            }
            for (ActivityDirective directive : directives) {
                if (directive.isArrives()) {
                    ActivityResolver.arrivalTargets(cast, directive);
                } else if (!directive.isReset() && !directive.isUnnamed() && !directive.isAll() && !directive.isModeAll()) {
                    ActivityResolver.resolveNames(cast, directive.namesText());
                }
            }
        } catch (IllegalArgumentException e) {
            issues.add(ScenarioIssue.error(where, e.getMessage()));
        }
    }

    /** {npc-only names} is alone on its line and names characters of the Cast (where it sits is checked when the scenario is parsed). */
    private static void checkNpcOnly(String where, String line, Cast cast, List<ScenarioIssue> issues) {
        try {
            List<String> found = NpcOnlyMarker.extract(line + "\n").namesTexts();
            if (found.isEmpty()) {
                issues.add(ScenarioIssue.error(where, "{npc-only} doit etre seul sur sa ligne : {npc-only Nom1, Nom2}"));
                return;
            }
            new StartingRoster(found.get(0)).outsideIds(cast);
        } catch (IllegalArgumentException e) {
            issues.add(ScenarioIssue.error(where, e.getMessage()));
        }
    }

    /** The lowercase variable a "{cast NAME ...}" line declares ; null for any other line, or a cast badly written (reported by checkLine). */
    private static String declaredBy(String line) {
        try {
            return CastCommand.parseLine(line).map(command -> command.variable().toLowerCase(Locale.ROOT)).orElse(null);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** variableUsed : the character is a %NAME% drawn later, so whether it has the tag cannot be known here (ChatScenario checks it once drawn). */
    private static void checkStory(String where, String line, Cast cast, List<ScenarioIssue> issues, boolean variableUsed) {
        try {
            List<StoryDirective> directives = StoryDirectiveMarker.extract(line + "\n").directives();
            if (directives.isEmpty()) {
                issues.add(ScenarioIssue.error(where, "{npc-story} doit etre seul sur sa ligne : {npc-story on|off nom tag}"));
            }
            for (StoryDirective directive : directives) {
                if (variableUsed) continue;
                String command = "{npc-story " + (directive.on() ? "on " : "off ") + directive.npcName() + " " + directive.tag() + "}";
                Optional<Npc> npc = cast.findByName(directive.npcName());
                if (directive.tag().equalsIgnoreCase("SECRET")) {
                    issues.add(ScenarioIssue.error(where, command + " : SECRET n'est pas un tag, le texte de \"# SECRET\" est permanent"));
                } else if (npc.isEmpty()) {
                    issues.add(ScenarioIssue.error(where, command + " : aucun personnage de ce nom dans characters/"));
                } else if (!npc.get().hasTag(directive.tag())) {
                    issues.add(ScenarioIssue.error(where, command + " : la fiche de " + npc.get().label()
                        + " n'a pas de section \"## " + directive.tag() + "\""));
                }
            }
        } catch (IllegalArgumentException e) {
            issues.add(ScenarioIssue.error(where, e.getMessage()));
        }
    }

    /**
     * A "{npc-delta ...}" starting at lines[first] : its text may span several lines, up to the line holding the closing
     * brace (or to the next heading if it is never closed). Returns the index of the last line it covers.
     */
    private static int checkDeltaBlock(String where, String[] lines, int first, Cast cast, List<ScenarioIssue> issues) {
        int last = first;
        while (lines[last].indexOf('}') < 0 && last + 1 < lines.length && !lines[last + 1].stripLeading().startsWith("#")) {
            last++;
        }
        String block = String.join("\n", java.util.Arrays.copyOfRange(lines, first, last + 1));
        boolean usesVariable = VARIABLE_USE.matcher(block).find();
        String checked = usesVariable ? VARIABLE_USE.matcher(block).replaceAll(Matcher.quoteReplacement(standIn(cast))) : block;
        checkDelta(where, checked, cast, issues);
        return last;
    }

    private static void checkDelta(String where, String line, Cast cast, List<ScenarioIssue> issues) {
        try {
            for (CharacterDelta delta : CharacterDeltaMarker.extract(line + "\n").deltas()) {
                if (cast.findByName(delta.npcName()).isEmpty()) {
                    issues.add(ScenarioIssue.error(where, "{npc-delta " + delta.npcName() + "} : aucun personnage de ce nom dans characters/"));
                }
            }
        } catch (IllegalArgumentException e) {
            issues.add(ScenarioIssue.error(where, e.getMessage()));
        }
    }

    /** Every command is fine on its own ; together, "unique" may still ask for more than a library holds. */
    private static void checkUniqueDoesNotRunOut(List<ScenarioSource> sources, PickLibrary library, Cast cast, List<ScenarioIssue> issues) {
        try {
            new PickResolver(library, bound -> 0, cast).resolve(sources.stream().map(ScenarioSource::text).toList());
        } catch (IllegalArgumentException | IllegalStateException e) {
            issues.add(ScenarioIssue.error("scenario", e.getMessage()));
        }
    }

    /** style.txt is sent to the LLM as it is : a command in it would never be executed, and an old-style one would leak. */
    public static ScenarioReport checkStyleText(String text) {
        return checkStyleText(text, STYLE_FILE);
    }

    /** Same check for any style file (style.txt, or styles/name.txt of a "{style name}"), issues reported under `where`. */
    public static ScenarioReport checkStyleText(String text, String where) {
        List<ScenarioIssue> issues = new ArrayList<>();
        try {
            LegacyCommandSyntax.requireNone(text);
        } catch (IllegalArgumentException e) {
            issues.add(ScenarioIssue.error(where, e.getMessage()));
        }
        checkChanceMarkers(where, text == null ? "" : text, issues);
        Matcher command = COMMAND_START.matcher(text == null ? "" : text);
        if (command.find()) {
            issues.add(ScenarioIssue.error(where, "les commandes entre accolades ne sont pas prises en charge dans "
                + where + " (" + command.group() + " ...) : ecrire le style en texte simple"));
        }
        return new ScenarioReport(issues);
    }

    /** {N%} chance markers of a style file or of a scenario text : one per line, 1 to 100, with a text to keep or drop. */
    private static void checkChanceMarkers(String where, String text, List<ScenarioIssue> issues) {
        for (String line : text.lines().toList()) {
            Matcher marker = ScenarioStyle.CHANCE_MARKER.matcher(line);
            int count = 0;
            while (marker.find()) {
                count++;
                int percent = Integer.parseInt(marker.group(1));
                if (percent < 1 || percent > 100) {
                    issues.add(ScenarioIssue.error(where, marker.group() + " : le pourcentage doit etre entre 1 et 100"));
                }
            }
            if (count > 1) {
                issues.add(ScenarioIssue.error(where, "plusieurs marqueurs de chance sur la meme ligne : " + line.strip()));
            }
            String rest = ScenarioStyle.CHANCE_MARKER.matcher(line).replaceAll("");
            if (count > 0 && rest.replaceAll("[\\s\\-*]", "").isEmpty()) {
                issues.add(ScenarioIssue.error(where, "marqueur de chance sans texte sur sa ligne : " + line.strip()));
            }
            if (Pattern.compile("\\{\\s*\\d").matcher(rest).find()) {
                issues.add(ScenarioIssue.error(where, "marqueur de chance invalide (attendu {20%}) : " + line.strip()));
            }
        }
    }

    /** Warnings about the parsed scenario. */
    public static ScenarioReport checkScenario(ChatScenario scenario) {
        List<ScenarioIssue> issues = new ArrayList<>();
        List<ScenarioAct> acts = scenario.acts();
        for (int i = 0; i < acts.size() - 1; i++) {
            ScenarioAct act = acts.get(i);
            if (!NextActMarker.isPresentIn(act.text())) {
                issues.add(ScenarioIssue.warning("acte " + act.number(),
                    "pas de condition [NEXT ACT] : le personnage avancera quand la scene lui semblera conclue"));
            }
        }
        if (scenario.style().text().length() > STYLE_WARNING_LENGTH) {
            issues.add(ScenarioIssue.warning(STYLE_FILE, scenario.style().text().length() + " caracteres : long pour un petit contexte"
                + " (au-dela de " + STYLE_WARNING_LENGTH + ", il pese sur chaque reponse)"));
        }
        Set<String> longSceneStyles = new HashSet<>();
        for (int i = 0; i < acts.size(); i++) {
            ScenarioAct act = acts.get(i);
            int length = scenario.styleAt(i + 1).text().length();
            if (act.sceneStyle().isPresent() && length > STYLE_WARNING_LENGTH && longSceneStyles.add(act.sceneStyle().text())) {
                issues.add(ScenarioIssue.warning("acte " + act.number(), "style.txt + {style} : " + length + " caracteres : long pour un petit"
                    + " contexte (au-dela de " + STYLE_WARNING_LENGTH + ", il pese sur chaque reponse)"));
            }
        }
        checkStories(scenario, issues);
        String opening = acts.isEmpty() ? scenario.premise() : scenario.premise() + "\n" + acts.get(0).text() + "\n" + String.join("\n", acts.get(0).beats());
        String openingNormalized = normalize(opening);
        Set<String> outsideAtStart = scenario.startingRoster().outsideIds(scenario.cast());
        for (Npc npc : scenario.cast().npcs()) {
            if (outsideAtStart.contains(npc.id())) {
                boolean arrivesSomeday = IntStream.rangeClosed(1, acts.size())
                    .anyMatch(act -> scenario.activityAt(act).inSceneIds().contains(npc.id()));
                if (!arrivesSomeday) {
                    issues.add(ScenarioIssue.warning("fiche " + npc.id(), npc.label() + " n'est pas dans {npc-only} et aucun "
                        + "{npc-arrives} ne le fait venir : il n'apparaitra jamais"));
                }
                continue; // not there at the start : no reason to be named in the opening
            }
            String firstWord = normalize(npc.label()).split("\\s+")[0];
            if (!firstWord.isEmpty() && !openingNormalized.contains(firstWord)) {
                issues.add(ScenarioIssue.warning(acts.isEmpty() ? "premisse" : "acte " + acts.get(0).number(),
                    npc.label() + " n'est pas cite : un personnage non presente peut rester sans repondre"));
            }
        }
        return new ScenarioReport(issues);
    }

    /** A "## tag" that no command ever switches on is dead text ; an "off" on a tag that is not on does nothing. */
    private static void checkStories(ChatScenario scenario, List<ScenarioIssue> issues) {
        Map<String, Set<String>> activeById = new HashMap<>();
        Set<String> everOn = new HashSet<>();
        for (ScenarioAct act : scenario.acts()) {
            for (StoryDirective directive : act.stories()) {
                Npc npc = scenario.cast().findByName(directive.npcName()).orElse(null);
                if (npc == null) continue;
                String tag = directive.tag().toLowerCase(Locale.ROOT);
                Set<String> active = activeById.computeIfAbsent(npc.id(), id -> new HashSet<>());
                if (directive.on()) {
                    active.add(tag);
                    everOn.add(npc.id() + "/" + tag);
                } else if (!active.remove(tag)) {
                    issues.add(ScenarioIssue.warning("acte " + act.number(), "{npc-story off " + directive.npcName() + " " + directive.tag()
                        + "} : ce tag n'est pas actif a cet endroit, la commande ne fait rien"));
                }
            }
        }
        for (Npc npc : scenario.cast().npcs()) {
            for (StoryTag tag : npc.tags()) {
                if (!everOn.contains(npc.id() + "/" + tag.name().toLowerCase(Locale.ROOT))) {
                    issues.add(ScenarioIssue.warning("fiche " + npc.id(), "le tag \"## " + tag.name()
                        + "\" n'est jamais active par un {npc-story on " + npc.label() + " " + tag.name() + "} : son texte ne sera jamais lu"));
                }
            }
        }
    }

    private static String normalize(String s) {
        return Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
}
