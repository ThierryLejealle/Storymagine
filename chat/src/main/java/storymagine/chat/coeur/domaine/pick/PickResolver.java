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

/**
 * Replaces every "[PICK: name]" and "[PICK-UNIQUE: name]" of a scenario's texts with a paragraph
 * drawn from the library "name". PICK draws independently each time ; PICK-UNIQUE never returns a
 * paragraph already handed out for that tag during the same resolution, by a PICK-UNIQUE or by a
 * plain PICK, and fails when the library has nothing left. A drawn paragraph is inserted as is :
 * a tag inside it is not resolved. Pure : same library and same draws, same result.
 */
public final class PickResolver {

    private static final Pattern TAG = Pattern.compile(
        "\\[(PICK(?:-UNIQUE)?)\\s*:\\s*([\\p{L}\\p{N}_-]+)\\s*\\]", Pattern.CASE_INSENSITIVE);

    private final PickLibrary library;
    private final RandomnessPort randomness;

    public PickResolver(PickLibrary library, RandomnessPort randomness) {
        this.library = library;
        this.randomness = randomness;
    }

    public static boolean containsTag(String text) {
        return TAG.matcher(text).find();
    }

    /**
     * Resolves the texts in order (they are one scenario : scenario.txt then its continuations),
     * sharing the "already drawn" memory between them.
     */
    public List<String> resolveAll(List<String> texts) {
        Map<PickTag, Set<Integer>> drawn = new HashMap<>();
        List<String> resolved = new ArrayList<>();
        for (String text : texts) {
            resolved.add(TAG.matcher(text).replaceAll(match -> Matcher.quoteReplacement(draw(match, drawn))));
        }
        return resolved;
    }

    private String draw(MatchResult match,Map<PickTag, Set<Integer>> drawn) {
        boolean unique = match.group(1).equalsIgnoreCase("PICK-UNIQUE");
        PickTag tag = new PickTag(match.group(2));
        PickParagraphs paragraphs = library.paragraphs(tag);
        Set<Integer> alreadyDrawn = drawn.computeIfAbsent(tag, t -> new HashSet<>());

        List<Integer> candidates = new ArrayList<>();
        for (int i = 0; i < paragraphs.size(); i++) {
            if (!unique || !alreadyDrawn.contains(i)) candidates.add(i);
        }
        if (candidates.isEmpty()) {
            throw new IllegalStateException("[PICK-UNIQUE: " + tag + "] : les " + paragraphs.size()
                + " paragraphe(s) de picks/" + tag + ".txt ont deja tous ete tires.");
        }
        int index = candidates.get(randomness.nextInt(candidates.size()));
        alreadyDrawn.add(index);
        return paragraphs.get(index);
    }
}
