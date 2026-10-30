package storymagine.chat.coeur.domaine.validation;

import storymagine.chat.coeur.domaine.pick.PickLibrary;
import storymagine.chat.coeur.domaine.pick.PickResolver;
import storymagine.chat.coeur.domaine.scenario.Cast;
import storymagine.chat.coeur.domaine.scenario.CharacterDelta;
import storymagine.chat.coeur.domaine.scenario.CharacterDeltaMarker;
import storymagine.chat.coeur.domaine.scenario.ChatScenario;
import storymagine.chat.coeur.domaine.scenario.LegacyCommandSyntax;
import storymagine.chat.coeur.domaine.scenario.NextActMarker;
import storymagine.chat.coeur.domaine.scenario.Npc;
import storymagine.chat.coeur.domaine.scenario.NpcOpening;
import storymagine.chat.coeur.domaine.scenario.NpcOpeningMarker;
import storymagine.chat.coeur.domaine.scenario.ScenarioAct;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Static checks of a scenario, no LLM involved. checkSources reads what the author wrote, line by
 * line, and collects EVERY problem (old bracket commands, unknown or malformed {commands}, a pick
 * library that is missing or too short, a character that does not exist, a command that must be
 * alone on its line) so one run lists all of them instead of stopping at the first. checkScenario
 * looks at the parsed scenario for things worth a warning (an act without a "[NEXT ACT]" condition,
 * a character the first act never mentions). Pure : same input, same report.
 */
public final class ScenarioValidator {

    private static final Set<String> COMMANDS = Set.of("pick", "npc-starts", "delta-pnj");
    private static final Pattern COMMAND_START = Pattern.compile("\\{\\s*([A-Za-z][A-Za-z-]*)");
    private static final Pattern COMMAND_TEXT = Pattern.compile("\\{[^}]*}");

    private static final String STYLE_FILE = "style.txt";
    /** Beyond this many characters the style advice starts to weigh on a small model's context. */
    static final int STYLE_WARNING_LENGTH = 1200;

    private ScenarioValidator() {}

    /** Errors only (warnings need the parsed scenario, see checkScenario). */
    public static ScenarioReport checkSources(List<ScenarioSource> sources, Cast cast, PickLibrary library) {
        List<ScenarioIssue> issues = new ArrayList<>();
        for (ScenarioSource source : sources) {
            String[] lines = source.text().replace("\r\n", "\n").replace("\r", "\n").split("\n", -1);
            for (int i = 0; i < lines.length; i++) {
                checkLine(source.fileName() + ":" + (i + 1), lines[i], cast, library, issues);
            }
        }
        if (issues.isEmpty()) checkUniqueDoesNotRunOut(sources, library, issues);
        return new ScenarioReport(issues);
    }

    private static void checkLine(String where, String line, Cast cast, PickLibrary library, List<ScenarioIssue> issues) {
        try {
            LegacyCommandSyntax.requireNone(line);
        } catch (IllegalArgumentException e) {
            issues.add(ScenarioIssue.error(where, e.getMessage()));
        }
        Matcher start = COMMAND_START.matcher(line);
        while (start.find()) {
            String name = start.group(1).toLowerCase(Locale.ROOT);
            Matcher text = COMMAND_TEXT.matcher(line.substring(start.start()));
            String command = text.find() ? text.group() : line.substring(start.start());
            if (!COMMANDS.contains(name)) {
                issues.add(ScenarioIssue.error(where, "commande inconnue {" + name + " ...} (commandes : pick, npc-starts, delta-pnj)"));
            } else if (name.equals("pick")) {
                checkPick(where, command, library, issues);
            } else if (name.equals("npc-starts")) {
                checkNpcStarts(where, line, cast, issues);
            } else {
                checkDelta(where, line, cast, issues);
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

    private static void checkNpcStarts(String where, String line, Cast cast, List<ScenarioIssue> issues) {
        NpcOpening opening = NpcOpeningMarker.extract(line + "\n").opening();
        if (opening.isNone()) {
            issues.add(ScenarioIssue.error(where, "{npc-starts} doit etre seul sur sa ligne : {npc-starts} ou {npc-starts Nom}"));
        } else if (opening.isNamed() && cast.findByName(opening.npcName()).isEmpty()) {
            issues.add(ScenarioIssue.error(where, "{npc-starts " + opening.npcName() + "} : aucun personnage de ce nom dans characters/"));
        }
    }

    private static void checkDelta(String where, String line, Cast cast, List<ScenarioIssue> issues) {
        try {
            for (CharacterDelta delta : CharacterDeltaMarker.extract(line + "\n").deltas()) {
                if (cast.findByName(delta.npcName()).isEmpty()) {
                    issues.add(ScenarioIssue.error(where, "{delta-pnj " + delta.npcName() + "} : aucun personnage de ce nom dans characters/"));
                }
            }
        } catch (IllegalArgumentException e) {
            issues.add(ScenarioIssue.error(where, e.getMessage()));
        }
    }

    /** Every command is fine on its own ; together, "unique" may still ask for more than a library holds. */
    private static void checkUniqueDoesNotRunOut(List<ScenarioSource> sources, PickLibrary library, List<ScenarioIssue> issues) {
        try {
            new PickResolver(library, bound -> 0).resolve(sources.stream().map(ScenarioSource::text).toList());
        } catch (IllegalArgumentException | IllegalStateException e) {
            issues.add(ScenarioIssue.error("scenario", e.getMessage()));
        }
    }

    /** style.txt is sent to the LLM as it is : a command in it would never be executed, and an old-style one would leak. */
    public static ScenarioReport checkStyleText(String text) {
        List<ScenarioIssue> issues = new ArrayList<>();
        try {
            LegacyCommandSyntax.requireNone(text);
        } catch (IllegalArgumentException e) {
            issues.add(ScenarioIssue.error(STYLE_FILE, e.getMessage()));
        }
        Matcher command = COMMAND_START.matcher(text == null ? "" : text);
        if (command.find()) {
            issues.add(ScenarioIssue.error(STYLE_FILE, "les commandes entre accolades ne sont pas prises en charge dans "
                + STYLE_FILE + " (" + command.group() + " ...) : ecrire le style en texte simple"));
        }
        return new ScenarioReport(issues);
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
        String opening = acts.isEmpty() ? scenario.premise() : scenario.premise() + "\n" + acts.get(0).text() + "\n" + String.join("\n", acts.get(0).beats());
        String openingNormalized = normalize(opening);
        for (Npc npc : scenario.cast().npcs()) {
            String firstWord = normalize(npc.label()).split("\\s+")[0];
            if (!firstWord.isEmpty() && !openingNormalized.contains(firstWord)) {
                issues.add(ScenarioIssue.warning(acts.isEmpty() ? "premisse" : "acte " + acts.get(0).number(),
                    npc.label() + " n'est pas cite : un personnage non presente peut rester sans repondre"));
            }
        }
        return new ScenarioReport(issues);
    }

    private static String normalize(String s) {
        return Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
}
