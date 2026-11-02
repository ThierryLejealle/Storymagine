package storymagine.chat.coeur.domaine.activity;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds and removes the "{npc-active|present|passive|absent names}", "{npc-reset}" and
 * "{npc-mode mode on|off names}" lines an author writes in an act (see ActivityDirective). A command
 * must be ALONE on its line. The lines are cut out of the text because it goes to every character :
 * a command left in it would be read as story. Any leftover "{npc-active" etc. the patterns do not
 * accept (in the middle of a sentence...) is an error, never silently kept in the text.
 */
public final class ActivityDirectiveMarker {

    private static final Pattern LEVEL_LINE = Pattern.compile(
        "^[ \\t]*\\{npc-(active|present|passive|absent)(?:[ \\t]+([^}\\n]*?))?[ \\t]*}[ \\t]*(?:\\n|$)",
        Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern RESET_LINE = Pattern.compile(
        "^[ \\t]*\\{npc-reset[ \\t]*}[ \\t]*(?:\\n|$)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern MODE_LINE = Pattern.compile(
        "^[ \\t]*\\{npc-mode[ \\t]+([^\\s}]+)[ \\t]+(on|off)[ \\t]+([^}\\n]+?)[ \\t]*}[ \\t]*(?:\\n|$)",
        Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern ANY = Pattern.compile(
        "\\{\\s*npc-(?:active|present|passive|absent|reset|mode)(?![\\p{L}\\p{N}_-])", Pattern.CASE_INSENSITIVE);

    /** text without any command line, plus the directives in the order written. */
    public record Extracted(String text, List<ActivityDirective> directives) {}

    private ActivityDirectiveMarker() {}

    public static Extracted extract(String text) {
        if (text == null) return new Extracted("", List.of());
        List<ActivityDirective> directives = new ArrayList<>();
        Matcher level = LEVEL_LINE.matcher(text);
        while (level.find()) {
            directives.add(ActivityDirective.level(
                NpcActivity.fromKeyword(level.group(1)).orElseThrow(), level.group(2)));
        }
        Matcher reset = RESET_LINE.matcher(text);
        while (reset.find()) directives.add(ActivityDirective.reset());
        Matcher modeLine = MODE_LINE.matcher(text);
        while (modeLine.find()) {
            NpcMode mode = NpcMode.fromKeyword(modeLine.group(1)).orElseThrow(() -> new IllegalArgumentException(
                "Mode inconnu : " + modeLine.group(1) + " dans " + modeLine.group().strip() + " (modes : " + NpcMode.keywords() + ")"));
            directives.add(modeLine.group(2).equalsIgnoreCase("on")
                ? ActivityDirective.modeOn(mode, modeLine.group(3)) : ActivityDirective.modeOff(mode, modeLine.group(3)));
        }
        String rest = MODE_LINE.matcher(RESET_LINE.matcher(LEVEL_LINE.matcher(text).replaceAll("")).replaceAll("")).replaceAll("");
        Matcher leftover = ANY.matcher(rest);
        if (leftover.find()) {
            String from = rest.substring(leftover.start());
            throw new IllegalArgumentException("Commande d'activite invalide : " + from.lines().findFirst().orElse(from)
                + " (attendu, seul sur sa ligne : {npc-active|present|passive|absent noms}, {npc-reset} ou {npc-mode mode on|off nom|*})");
        }
        return new Extracted(rest, directives);
    }
}
