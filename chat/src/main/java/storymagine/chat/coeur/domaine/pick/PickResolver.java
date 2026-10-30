package storymagine.chat.coeur.domaine.pick;

import storymagine.chat.coeur.ports.RandomnessPort;

import java.util.ArrayList;
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
 * as is : a command inside it is not resolved. Pure : same library and same draws, same result.
 */
public final class PickResolver {

    /** Any "{pick ...}" — a malformed one is reported by draw(), never silently left in the text. */
    private static final Pattern COMMAND = Pattern.compile("\\{\\s*pick(?![\\p{L}\\p{N}_-])([^}]*)}", Pattern.CASE_INSENSITIVE);

    private static final String PARAGRAPH_SEPARATOR = "\n\n";

    private final PickLibrary library;
    private final RandomnessPort randomness;

    public PickResolver(PickLibrary library, RandomnessPort randomness) {
        this.library = library;
        this.randomness = randomness;
    }

    public static boolean containsTag(String text) {
        return COMMAND.matcher(text).find();
    }

    /**
     * Resolves the texts in order (they are one scenario : scenario.txt then its continuations),
     * sharing the "already drawn" memory between them.
     */
    public List<String> resolveAll(List<String> texts) {
        return resolve(texts).texts();
    }

    /** Same as resolveAll, and keeps the draws made, in the order of the commands. */
    public PickResolution resolve(List<String> texts) {
        Map<PickTag, Set<Integer>> drawn = new HashMap<>();
        List<PickDraw> draws = new ArrayList<>();
        List<String> resolved = new ArrayList<>();
        for (String text : texts) {
            resolved.add(COMMAND.matcher(text).replaceAll(match -> Matcher.quoteReplacement(draw(match, drawn, draws))));
        }
        return new PickResolution(resolved, new PickDraws(draws));
    }

    private String draw(MatchResult match, Map<PickTag, Set<Integer>> drawn, List<PickDraw> draws) {
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
        draws.add(new PickDraw(mode, tag, handedOut));
        return String.join(PARAGRAPH_SEPARATOR, handedOut);
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
                        + " paragraphe(s) de picks/" + tag + ".txt ont deja tous ete tires.");
                }
                return drawFrom(notYet, 1);
            default:
                if (mode.count() > size) {
                    throw new IllegalStateException("{pick " + mode.count() + " " + tag + "} : picks/" + tag
                        + ".txt ne contient que " + size + " paragraphe(s).");
                }
                return drawFrom(everything, mode.count());
        }
    }

    /** count different candidates, each drawn at random among the ones left. */
    private List<Integer> drawFrom(List<Integer> candidates, int count) {
        List<Integer> left = new ArrayList<>(candidates);
        List<Integer> chosen = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            chosen.add(left.remove(randomness.nextInt(left.size())));
        }
        return chosen;
    }
}
