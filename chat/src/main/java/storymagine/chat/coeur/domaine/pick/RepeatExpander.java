package storymagine.chat.coeur.domaine.pick;

import storymagine.chat.coeur.ports.RandomnessPort;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/**
 * Unfolds the "{repeat order|random [N] name}" ... "{end-repeat}" blocks of a scenario's texts into
 * ordinary text, before anything else reads them : the block is a template written once, copied
 * once per paragraph taken from the library "name" (the whole file without N), "{n}" becoming the
 * copy's number (1, 2, ...), "{item}" its whole paragraph and "{item1}", "{item2}"... its pieces (see
 * PickParagraph). Both commands stand alone on their line, blocks do not nest, and "{n}" or "{item}"
 * outside a block is an error. A paragraph with a piece count different from what the block uses is an
 * error too. The unfolded text is what the game reads : the repeat never reaches it. Pure : same
 * library and same draws, same result.
 */
public final class RepeatExpander {

    private static final Pattern START = Pattern.compile("\\{\\s*repeat(?![\\p{L}\\p{N}_-])([^}]*)}", Pattern.CASE_INSENSITIVE);
    private static final Pattern END = Pattern.compile("\\{\\s*end-repeat\\s*}", Pattern.CASE_INSENSITIVE);
    /** "{n}" (group 1) or "{item}" / "{itemK}" (group 2 : the K, empty for the whole paragraph). */
    private static final Pattern ZONE = Pattern.compile("\\{\\s*(?:(n)|item(\\d*))\\s*}", Pattern.CASE_INSENSITIVE);

    private static final String BLOCK_SEPARATOR = "\n\n";

    private final PickLibrary library;
    private final RandomnessPort randomness;

    public RepeatExpander(PickLibrary library, RandomnessPort randomness) {
        this.library = library;
        this.randomness = randomness;
    }

    public static boolean containsTag(String text) {
        return START.matcher(text).find() || END.matcher(text).find();
    }

    public RepeatExpansion expand(List<String> texts) {
        List<PickDraw> draws = new ArrayList<>();
        List<String> expanded = new ArrayList<>();
        for (String text : texts) {
            expanded.add(expandText(text, draws));
        }
        return new RepeatExpansion(expanded, draws);
    }

    private String expandText(String text, List<PickDraw> draws) {
        if (!containsTag(text) && !ZONE.matcher(text).find()) return text;
        List<String> out = new ArrayList<>();
        List<String> body = null;
        String header = null;
        int headerLine = 0;
        String[] lines = text.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int number = i + 1;
            if (START.matcher(line).find()) {
                requireAlone(START, line, number);
                if (body != null) throw error(number, "un {repeat} ne peut pas contenir un autre {repeat} (celui de la ligne " + headerLine + ")");
                header = line.strip();
                headerLine = number;
                body = new ArrayList<>();
            } else if (END.matcher(line).find()) {
                requireAlone(END, line, number);
                if (body == null) throw error(number, "{end-repeat} sans {repeat} avant");
                out.add(unfold(header, headerLine, body, draws));
                body = null;
            } else if (body != null) {
                body.add(line);
            } else {
                if (ZONE.matcher(line).find()) throw error(number, ZONE.matcher(line).results().findFirst().orElseThrow().group() + " en dehors d'un bloc {repeat}");
                out.add(line);
            }
        }
        if (body != null) throw error(headerLine, header + " sans {end-repeat}");
        return String.join("\n", out);
    }

    private static void requireAlone(Pattern command, String line, int number) {
        if (!command.matcher(line.strip()).matches()) {
            throw error(number, "{repeat ...} et {end-repeat} doivent etre seuls sur leur ligne : " + line.strip());
        }
    }

    private String unfold(String header, int headerLine, List<String> body, List<PickDraw> draws) {
        if (body.stream().allMatch(String::isBlank)) throw error(headerLine, header + " : bloc vide");
        Matcher command = START.matcher(header);
        command.matches();
        String[] words = command.group(1).strip().split("\\s+");
        if (words.length < 2 || words.length > 3) {
            throw error(headerLine, "Commande repeat invalide : " + header + " (attendu : {repeat order|random [N] nom})");
        }
        RepeatMode mode;
        PickTag tag;
        PickParagraphs paragraphs;
        try {
            mode = RepeatMode.parse(words[0], words.length == 3 ? words[1] : null);
            tag = new PickTag(words[words.length - 1]);
            paragraphs = library.paragraphs(tag);
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw error(headerLine, e.getMessage());
        }
        int count = mode.takesEverything() ? paragraphs.size() : mode.count();
        if (count > paragraphs.size()) {
            throw error(headerLine, header + " : picks/" + tag + ".txt ne contient que " + paragraphs.size() + " paragraphe(s).");
        }
        String template = String.join("\n", body);
        int piecesNeeded = piecesNeeded(template, headerLine);

        List<String> instances = new ArrayList<>();
        List<String> taken = new ArrayList<>();
        List<Integer> indexes = indexes(mode, paragraphs.size(), count);
        for (int i = 0; i < indexes.size(); i++) {
            PickParagraph paragraph = new PickParagraph(paragraphs.get(indexes.get(i)));
            if (piecesNeeded > 0 && paragraph.pieces().size() != piecesNeeded) {
                throw error(headerLine, header + " : le paragraphe \"" + paragraph.preview() + "\" de picks/" + tag + ".txt a "
                    + paragraph.pieces().size() + " morceau(x) (separes par une ligne '-'), le bloc en utilise " + piecesNeeded + ".");
            }
            instances.add(render(template, i + 1, paragraph));
            taken.add(paragraph.text());
        }
        draws.add(new PickDraw(mode.recordedMode(), tag, taken));
        return String.join(BLOCK_SEPARATOR, instances);
    }

    private List<Integer> indexes(RepeatMode mode, int size, int count) {
        List<Integer> everything = IntStream.range(0, size).boxed().toList();
        return mode.order() == RepeatMode.Order.ORDER ? everything.subList(0, count) : RandomSelection.draw(randomness, everything, count);
    }

    /** The highest K of the "{itemK}" the block uses ; 0 when it uses none. */
    private static int piecesNeeded(String template, int headerLine) {
        int highest = 0;
        Matcher zone = ZONE.matcher(template);
        while (zone.find()) {
            String digits = zone.group(2);
            if (digits == null || digits.isEmpty()) continue;
            int k = Integer.parseInt(digits);
            if (k < 1) throw error(headerLine, zone.group() + " : les morceaux se numerotent a partir de 1");
            highest = Math.max(highest, k);
        }
        return highest;
    }

    /** One pass : what a paragraph contains is inserted as it is, never read for a zone again. */
    private static String render(String template, int number, PickParagraph paragraph) {
        List<String> pieces = paragraph.pieces();
        return ZONE.matcher(template).replaceAll(zone -> Matcher.quoteReplacement(
            zone.group(1) != null ? Integer.toString(number)
                : zone.group(2).isEmpty() ? paragraph.withoutSeparators()
                : pieces.get(Integer.parseInt(zone.group(2)) - 1))).strip();
    }

    private static IllegalArgumentException error(int line, String message) {
        return new IllegalArgumentException("ligne " + line + " : " + message);
    }
}
