package storymagine.chat.coeur.domaine.pick;

import storymagine.chat.coeur.domaine.scenario.Cast;
import storymagine.chat.coeur.domaine.scenario.Npc;
import storymagine.chat.coeur.ports.RandomnessPort;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/**
 * Unfolds the "{repeat ...}" ... "{end-repeat}" blocks of a scenario's texts into ordinary text, before
 * anything else reads them : the block is a template written once, copied once per item, "{n}" becoming
 * the copy's number (1, 2, ...), "{item}" its whole item and "{item1}", "{item2}"... its pieces (see
 * PickParagraph). Three kinds of block : "{repeat order|random [N] name}" takes its items from the library
 * "name" (the whole file without N), "{repeat order|random [N] cast NAME [(jack, mia)]}" takes characters
 * (the listed ones, or the whole cast) and keeps the copy's character in the variable NAME (see
 * CastResolver), "{repeat N}" just makes N copies, without items. Both commands stand alone on their line,
 * blocks do not nest, and "{n}" or "{item}" outside a block is an error. A paragraph with a piece count
 * different from what the block uses is an error too. A copy that holds a cast (or binds a variable) is
 * wrapped in two internal lines CastResolver reads, so what a copy declares is forgotten at its end. The
 * unfolded text is what the game reads : the repeat never reaches it. Pure : same library and same draws,
 * same result.
 */
public final class RepeatExpander {

    private static final Pattern START = Pattern.compile("\\{\\s*repeat(?![\\p{L}\\p{N}_-])([^}]*)}", Pattern.CASE_INSENSITIVE);
    private static final Pattern END = Pattern.compile("\\{\\s*end-repeat\\s*}", Pattern.CASE_INSENSITIVE);
    /** "{n}" (group 1) or "{item}" / "{itemK}" (group 2 : the K, empty for the whole paragraph). */
    private static final Pattern ZONE = Pattern.compile("\\{\\s*(?:(n)|item(\\d*))\\s*}", Pattern.CASE_INSENSITIVE);
    private static final Pattern LIST = Pattern.compile("\\(([^)]*)\\)");
    private static final String SYNTAX = "{repeat N}, {repeat order|random [N] nom} ou {repeat order|random [N] cast NOM [(jack, mia)]}";

    private static final String BLOCK_SEPARATOR = "\n\n";

    private final PickLibrary library;
    private final RandomnessPort randomness;
    private final Cast cast;

    public RepeatExpander(PickLibrary library, RandomnessPort randomness) {
        this(library, randomness, new Cast(List.of()));
    }

    public RepeatExpander(PickLibrary library, RandomnessPort randomness, Cast cast) {
        this.library = library;
        this.randomness = randomness;
        this.cast = cast;
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

    /** What a block repeats over : how many copies, the item of each (none for "{repeat N}"), the variable it binds, if any. */
    private record Items(List<String> texts, List<String> npcIds, int copies, String variable, String where) {}

    private String unfold(String header, int headerLine, List<String> body, List<PickDraw> draws) {
        if (body.stream().allMatch(String::isBlank)) throw error(headerLine, header + " : bloc vide");
        Matcher command = START.matcher(header);
        command.matches();
        String arguments = command.group(1).strip();
        List<String> listed = null;
        Matcher list = LIST.matcher(arguments);
        if (list.find()) {
            try {
                listed = CastCommand.candidatesOf(list.group(1), header);
            } catch (IllegalArgumentException e) {
                throw error(headerLine, e.getMessage());
            }
            arguments = (arguments.substring(0, list.start()) + arguments.substring(list.end())).strip();
        }
        String[] words = arguments.isEmpty() ? new String[0] : arguments.split("\\s+");
        String template = String.join("\n", body);
        int piecesNeeded = piecesNeeded(template, headerLine);

        Items items;
        try {
            items = itemsOf(header, words, listed, draws);
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw error(headerLine, e.getMessage());
        }
        if (items.texts().isEmpty() && usesItem(template)) {
            throw error(headerLine, header + " : {item} n'a pas de sens dans un {repeat N}, qui n'a pas d'elements");
        }

        boolean scoped = items.variable() != null || CastCommand.appearsIn(template);
        List<String> instances = new ArrayList<>();
        for (int i = 0; i < items.copies(); i++) {
            String instance;
            if (items.texts().isEmpty()) {
                instance = render(template, i + 1, new PickParagraph(""));
            } else {
                PickParagraph paragraph = new PickParagraph(items.texts().get(i));
                if (piecesNeeded > 0 && paragraph.pieces().size() != piecesNeeded) {
                    throw error(headerLine, header + " : le paragraphe \"" + paragraph.preview() + "\" de " + items.where() + " a "
                        + paragraph.pieces().size() + " morceau(x) (separes par une ligne '-'), le bloc en utilise " + piecesNeeded + ".");
                }
                instance = render(template, i + 1, paragraph);
            }
            if (scoped) {
                String bind = items.variable() == null ? "" : CastResolver.bindLine(items.variable(), items.npcIds().get(i)) + "\n";
                instance = CastResolver.COPY_START + "\n" + bind + instance + "\n" + CastResolver.COPY_END;
            }
            instances.add(instance);
        }
        return String.join(BLOCK_SEPARATOR, instances);
    }

    /** The items of a block from the words after "repeat" ; adds the draw it makes to `draws`. */
    private Items itemsOf(String header, String[] words, List<String> listed, List<PickDraw> draws) {
        if (words.length == 1 && words[0].matches("\\d{1,6}")) {
            if (listed != null) throw new IllegalArgumentException(header + " : une liste entre parentheses n'a de sens qu'avec cast");
            int copies = Integer.parseInt(words[0]);
            if (copies < 1) throw new IllegalArgumentException("Nombre de repeat invalide : '" + words[0] + "' (entier superieur ou egal a 1)");
            return new Items(List.of(), List.of(), copies, null, "");
        }
        if (words.length < 2 || words.length > 4) {
            throw new IllegalArgumentException("Commande repeat invalide : " + header + " (attendu : " + SYNTAX + ")");
        }
        boolean castSource = words.length >= 3 && words[words.length - 2].equalsIgnoreCase("cast");
        String countWord = words.length == (castSource ? 4 : 3) ? words[1] : null;
        if (words.length == 4 && !castSource) {
            throw new IllegalArgumentException("Commande repeat invalide : " + header + " (attendu : " + SYNTAX + ")");
        }
        RepeatMode mode = RepeatMode.parse(words[0], countWord);
        if (castSource) return castItems(header, words[words.length - 1], listed, mode, draws);
        if (listed != null) throw new IllegalArgumentException(header + " : une liste entre parentheses n'a de sens qu'avec cast");

        PickTag tag = new PickTag(words[words.length - 1]);
        PickParagraphs paragraphs = library.paragraphs(tag);
        int count = mode.takesEverything() ? paragraphs.size() : mode.count();
        if (count > paragraphs.size()) {
            throw new IllegalArgumentException(header + " : " + tag.file() + " ne contient que " + paragraphs.size() + " paragraphe(s).");
        }
        List<Integer> indexes = indexes(mode, paragraphs.size(), count);
        List<String> taken = indexes.stream().map(paragraphs::get).toList();
        draws.add(new PickDraw(mode.recordedMode(), tag, taken));
        return new Items(taken, List.of(), indexes.size(), null, tag.file());
    }

    private Items castItems(String header, String variable, List<String> listed, RepeatMode mode, List<PickDraw> draws) {
        if (!CastCommand.VARIABLE_NAME.matcher(variable).matches()) {
            throw new IllegalArgumentException("Nom de variable invalide : '" + variable + "' dans " + header
                + " (une lettre puis lettres, chiffres ou _)");
        }
        List<Npc> pool = CastPool.of(listed == null ? List.of() : listed, cast);
        int count = mode.takesEverything() ? pool.size() : mode.count();
        if (count > pool.size()) {
            throw new IllegalArgumentException(header + " : il n'y a que " + pool.size() + " personnage(s) a tirer.");
        }
        List<Integer> indexes = indexes(mode, pool.size(), count);
        List<String> labels = indexes.stream().map(i -> pool.get(i).label()).toList();
        draws.add(new PickDraw(mode.recordedMode(), new PickTag(variable), labels));
        return new Items(labels, indexes.stream().map(i -> pool.get(i).id()).toList(), indexes.size(), variable, "cast " + variable);
    }

    private List<Integer> indexes(RepeatMode mode, int size, int count) {
        List<Integer> everything = IntStream.range(0, size).boxed().toList();
        return mode.order() == RepeatMode.Order.ORDER ? everything.subList(0, count) : RandomSelection.draw(randomness, everything, count);
    }

    private static boolean usesItem(String template) {
        return ZONE.matcher(template).results().anyMatch(zone -> zone.group(2) != null);
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
