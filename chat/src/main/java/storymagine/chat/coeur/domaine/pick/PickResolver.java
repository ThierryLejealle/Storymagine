package storymagine.chat.coeur.domaine.pick;

import storymagine.chat.coeur.domaine.scenario.Cast;
import storymagine.chat.coeur.ports.RandomnessPort;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/**
 * Replaces every "{pick mode name}" command of a scenario's texts with paragraphs drawn from the
 * library "name" (see PickMode : random, unique, all, or a number N). Whatever the mode, what a
 * command hands out counts as "already drawn" for a later "unique" on the same library, which fails
 * when nothing is left. Several paragraphs are joined by a blank line. A drawn paragraph is inserted
 * as is : a command inside it is not resolved. The "{repeat}" blocks are unfolded first (see
 * RepeatExpander), so a "{pick}" written in a block is drawn once per copy. The "{cast}" commands run
 * last (see CastResolver). Pure : same library and same draws, same result.
 */
public final class PickResolver {

    /** Any "{pick ...}" — a malformed one is reported by draw(), never silently left in the text. */
    private static final Pattern COMMAND = Pattern.compile("\\{\\s*pick(?![\\p{L}\\p{N}_-])([^}]*)}", Pattern.CASE_INSENSITIVE);

    private static final String PARAGRAPH_SEPARATOR = "\n\n";

    private final PickLibrary library;
    private final RandomnessPort randomness;
    private final Cast cast;

    public PickResolver(PickLibrary library, RandomnessPort randomness) {
        this(library, randomness, new Cast(List.of()));
    }

    public PickResolver(PickLibrary library, RandomnessPort randomness, Cast cast) {
        this.library = library;
        this.randomness = randomness;
        this.cast = cast;
    }

    public static boolean containsTag(String text) {
        return COMMAND.matcher(text).find() || RepeatExpander.containsTag(text) || CastCommand.appearsIn(text);
    }

    /**
     * Resolves the texts in order (they are one scenario : scenario.txt then its continuations),
     * sharing the "already drawn" memory between them.
     */
    public List<String> resolveAll(List<String> texts) {
        return resolve(texts).texts();
    }

    /**
     * Same as resolveAll, and keeps the draws made, in reading order : a copy of a "{repeat}" block groups its
     * own item, the picks written in it and its casts, so they can be shown copy by copy.
     */
    public PickResolution resolve(List<String> texts) {
        RepeatExpansion unfolded = new RepeatExpander(library, randomness, cast).expand(texts);
        Map<PickTag, Set<Integer>> drawn = new HashMap<>();
        List<Positioned> picks = new ArrayList<>();
        List<String> resolved = new ArrayList<>();
        for (int t = 0; t < unfolded.texts().size(); t++) {
            int textIndex = t;
            resolved.add(COMMAND.matcher(unfolded.texts().get(t)).replaceAll(match -> {
                PickDraw draw = draw(match, drawn, scopeAt(unfolded.spans(), textIndex, match.start()));
                picks.add(new Positioned(draw, textIndex, match.start()));
                return Matcher.quoteReplacement(String.join(PARAGRAPH_SEPARATOR, draw.paragraphs()));
            }));
        }
        // Last : the "{cast}" commands and their %NAME% variables, so a variable written in a paragraph drawn above is replaced too.
        List<PickDraw> casts = List.of();
        if (resolved.stream().anyMatch(CastResolver::isNeeded)) {
            CastResolver.Resolution casted = new CastResolver(cast, randomness).resolve(resolved);
            resolved = new ArrayList<>(casted.texts());
            casts = casted.draws();
        }
        return new PickResolution(resolved, new PickDraws(inReadingOrder(unfolded.spans(), picks, casts)));
    }

    private static PickScope scopeAt(List<RepeatExpansion.CopySpan> spans, int textIndex, int position) {
        return spans.stream().filter(span -> span.contains(textIndex, position)).map(RepeatExpansion.CopySpan::scope)
            .findFirst().orElse(PickScope.NONE);
    }

    /** A pick draw and where its command stood in the unfolded texts. */
    private record Positioned(PickDraw draw, int textIndex, int position) {}

    /** What is shown together : one copy of a block (its item, then its picks, then its casts) or one pick outside any copy. */
    private static final class Group {
        final int textIndex;
        final int position;
        final List<PickDraw> draws = new ArrayList<>();

        Group(int textIndex, int position) {
            this.textIndex = textIndex;
            this.position = position;
        }
    }

    /** Groups in the order they stand in the texts ; casts outside any copy come last (they run after everything). */
    private static List<PickDraw> inReadingOrder(List<RepeatExpansion.CopySpan> spans, List<Positioned> picks, List<PickDraw> casts) {
        List<Group> groups = new ArrayList<>();
        Map<PickScope, Group> copies = new HashMap<>();
        for (RepeatExpansion.CopySpan span : spans) {
            Group group = new Group(span.textIndex(), span.start());
            if (span.itemDraw() != null) group.draws.add(span.itemDraw());
            groups.add(group);
            copies.put(span.scope(), group);
        }
        for (Positioned pick : picks) {
            Group group = pick.draw().scope().isNone() ? null : copies.get(pick.draw().scope());
            if (group == null) {
                group = new Group(pick.textIndex(), pick.position());
                groups.add(group);
            }
            group.draws.add(pick.draw());
        }
        List<PickDraw> loose = new ArrayList<>();
        for (PickDraw cast : casts) {
            Group group = cast.scope().isNone() ? null : copies.get(cast.scope());
            if (group == null) loose.add(cast); else group.draws.add(cast);
        }
        groups.sort(Comparator.comparingInt((Group g) -> g.textIndex).thenComparingInt(g -> g.position));
        List<PickDraw> ordered = new ArrayList<>();
        groups.forEach(group -> ordered.addAll(group.draws));
        ordered.addAll(loose);
        return ordered;
    }

    private PickDraw draw(MatchResult match, Map<PickTag, Set<Integer>> drawn, PickScope scope) {
        String[] words = match.group(1).strip().split("\\s+");
        if (words.length != 2 || words[0].isEmpty()) {
            throw new IllegalArgumentException("Commande pick invalide : " + match.group()
                + " (attendu : {pick random|unique|all|N nom})");
        }
        PickMode mode = PickMode.parse(words[0]);
        PickTag tag = new PickTag(words[1]);
        PickParagraphs paragraphs = library.paragraphs(tag);
        Set<Integer> alreadyDrawn = drawn.computeIfAbsent(tag, t -> new HashSet<>());

        List<Integer> chosen = choose(mode, tag, paragraphs.size(), alreadyDrawn);
        alreadyDrawn.addAll(chosen);
        List<String> handedOut = chosen.stream().map(paragraphs::get).toList();
        return new PickDraw(mode, tag, handedOut, scope);
    }

    private List<Integer> choose(PickMode mode, PickTag tag, int size, Set<Integer> alreadyDrawn) {
        List<Integer> everything = IntStream.range(0, size).boxed().toList();
        switch (mode.kind()) {
            case ALL:
                return everything;
            case RANDOM:
                return drawFrom(everything, 1);
            case UNIQUE:
                List<Integer> notYet = everything.stream().filter(i -> !alreadyDrawn.contains(i)).toList();
                if (notYet.isEmpty()) {
                    throw new IllegalStateException("{pick unique " + tag + "} : les " + size
                        + " paragraphe(s) de " + tag.file() + " ont deja tous ete tires.");
                }
                return drawFrom(notYet, 1);
            default:
                if (mode.count() > size) {
                    throw new IllegalStateException("{pick " + mode.count() + " " + tag + "} : " + tag.file()
                        + " ne contient que " + size + " paragraphe(s).");
                }
                return drawFrom(everything, mode.count());
        }
    }

    private List<Integer> drawFrom(List<Integer> candidates, int count) {
        return RandomSelection.draw(randomness, candidates, count);
    }
}
