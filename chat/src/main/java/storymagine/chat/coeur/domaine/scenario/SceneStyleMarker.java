package storymagine.chat.coeur.domaine.scenario;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds and removes the "{style name}" lines an author writes under a heading to give that scene (and the
 * scenes below it) an extra style advice, read from styles/name.txt (see ScenarioStyle), or in the premise
 * to add it to style.txt for the whole story. Several may follow each other (their styles add up in the
 * order written). Each must be ALONE on
 * its line, and is cut out of the text because the text goes to every character. The name may hold letters,
 * digits, "_" and "-" (foret-sombre). Any "{style" the pattern does not accept (mid-sentence, no name, a
 * path...) is an error, never silently kept.
 */
public final class SceneStyleMarker {

    private static final Pattern LINE = Pattern.compile(
        "^[ \\t]*\\{style[ \\t]+([\\p{L}\\p{N}_-]+)[ \\t]*}[ \\t]*(?:\\n|$)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern ANY = Pattern.compile("\\{\\s*style(?![\\p{L}\\p{N}_-])", Pattern.CASE_INSENSITIVE);

    /** text without any command line, plus the name of each command found (in the order written). */
    public record Extracted(String text, List<String> names) {}

    private SceneStyleMarker() {}

    public static Extracted extract(String text) {
        if (text == null) return new Extracted("", List.of());
        List<String> found = new ArrayList<>();
        Matcher matcher = LINE.matcher(text);
        while (matcher.find()) found.add(matcher.group(1));
        String rest = LINE.matcher(text).replaceAll("");
        Matcher leftover = ANY.matcher(rest);
        if (leftover.find()) {
            String from = rest.substring(leftover.start());
            throw new IllegalArgumentException("Commande {style} invalide : " + from.lines().findFirst().orElse(from)
                + " (attendu, seul sur sa ligne : {style nom}, avec un nom de lettres, chiffres, - ou _ ; le fichier est styles/nom.txt)");
        }
        return new Extracted(rest, found);
    }
}
