package storymagine.chat.infra;

import storymagine.chat.coeur.domaine.activity.ActivityDirective;
import storymagine.chat.coeur.domaine.activity.ActivityDirectiveMarker;
import storymagine.chat.coeur.domaine.scenario.ActNumber;
import storymagine.chat.coeur.domaine.scenario.CharacterDelta;
import storymagine.chat.coeur.domaine.scenario.CharacterDeltaMarker;
import storymagine.chat.coeur.domaine.scenario.LegacyCommandSyntax;
import storymagine.chat.coeur.domaine.scenario.NpcOnlyMarker;
import storymagine.chat.coeur.domaine.scenario.NpcOpening;
import storymagine.chat.coeur.domaine.scenario.NpcOpeningMarker;
import storymagine.chat.coeur.domaine.scenario.SceneStyleMarker;
import storymagine.chat.coeur.domaine.scenario.ScenarioAct;
import storymagine.chat.coeur.domaine.scenario.ScenarioStyle;
import storymagine.chat.coeur.domaine.scenario.StartingRoster;
import storymagine.chat.coeur.domaine.scenario.StoryDirective;
import storymagine.chat.coeur.domaine.scenario.StoryDirectiveMarker;
import storymagine.chat.coeur.domaine.scenario.Teaser;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a single scenario.txt into a premise plus a flat list of leaf ScenarioAct — see
 * ChatFileStorageAdapter for the file convention. Acts are written as a nested Markdown outline
 * (#, ##, ###...) : only leaves (headings with no sub-heading) are ever "current" ; a parent
 * heading's own body is inherited by all its descendants (folded into each leaf's resolved text).
 * A parent's "[...]" story beats fire only once, the first time a leaf under it becomes current —
 * not on every later sibling transition — hence the per-leaf beat diffing against the previous
 * leaf's ancestor path below.
 */
final class ScenarioOutlineParser {

    private static final Pattern HEADING = Pattern.compile("^(#{1,6})\\s*(.*)$");
    private static final String  SCENARIO_MARKER = "SCENARIO";

    private ScenarioOutlineParser() {}

    /**
     * premiseOpening : who speaks first when the story begins ("{npc-starts}" in the premise, already cut out of premise).
     * premiseStyle : the styles of the "{style name}" lines of the premise (cut out too), to add to style.txt for the whole story.
     */
    record Outline(String premise, List<ScenarioAct> acts, NpcOpening premiseOpening, StartingRoster startingRoster,
                   ScenarioStyle premiseStyle) {}

    /** For a scenario text with no "{style name}" : a name here has no styles/ directory to be read from. */
    static Outline parse(String rawInput) {
        return parse(rawInput, name -> {
            throw new IllegalArgumentException("{style " + name + "} : styles/" + name + ".txt est introuvable.");
        });
    }

    /** styleReader gives the style of a "{style name}" (the adapter reads styles/name.txt) or throws when it cannot. */
    static Outline parse(String rawInput, Function<String, ScenarioStyle> styleReader) {
        // Normalise CRLF/CR -> LF avant tout decoupage sur "\n" : sans ca, chaque ligne garde un
        // '\r' final invisible, et HEADING.matches() echoue TOUJOURS dessus — '.' n'inclut pas '\r'
        // par defaut en Java, donc "(.*)$" ne peut jamais l'absorber. Resultat observe en vrai : un
        // fichier scenario.txt enregistre avec des fins de ligne Windows (CRLF) perdait tous ses
        // titres "#"/"##", scenario.acts() se retrouvait vide, et le fallback "pas d'actes" (voir
        // ChatSession.fresh) deversait toute la premisse d'un coup — voir evols/2026-07-17-...
        String raw = rawInput.replace("\r\n", "\n").replace("\r", "\n");
        LegacyCommandSyntax.requireNone(raw);
        List<Heading> headings = splitHeadings(raw);
        List<String> onlyTexts = new ArrayList<>();
        if (headings.isEmpty()) {
            SceneStyleMarker.Extracted style = SceneStyleMarker.extract(raw);
            NpcOnlyMarker.Extracted only = NpcOnlyMarker.extract(style.text());
            onlyTexts.addAll(only.namesTexts());
            NpcOpeningMarker.Extracted premise = NpcOpeningMarker.extract(only.text());
            requireNoDelta(premise.text());
            return new Outline(premise.text().strip(), List.of(), premise.opening(), rosterOf(onlyTexts),
                styleOf(style.names(), styleReader));
        }

        Heading first = headings.get(0);
        String premiseText;
        List<Heading> actHeadings;
        if (first.depth == 1 && first.title.strip().equalsIgnoreCase(SCENARIO_MARKER)) {
            premiseText = first.body.strip();
            actHeadings = headings.subList(1, headings.size());
        } else {
            premiseText = textBefore(raw, first).strip();
            actHeadings = headings;
        }
        SceneStyleMarker.Extracted premiseStyle = SceneStyleMarker.extract(premiseText);
        NpcOnlyMarker.Extracted premiseOnly = NpcOnlyMarker.extract(premiseStyle.text());
        onlyTexts.addAll(premiseOnly.namesTexts());
        NpcOpeningMarker.Extracted premise = NpcOpeningMarker.extract(premiseOnly.text());
        requireNoDelta(premise.text());

        List<Node> roots = buildTree(actHeadings, onlyTexts);
        List<ScenarioAct> leaves = new ArrayList<>();
        for (Node root : roots) flatten(root, new ArrayList<>(), leaves, new ArrayList<>(), styleReader);
        return new Outline(premise.text().strip(), leaves, premise.opening(), rosterOf(onlyTexts),
            styleOf(premiseStyle.names(), styleReader));
    }

    /** At most one "{npc-only}" in the whole scenario : none means everybody is there at the start. */
    private static StartingRoster rosterOf(List<String> onlyTexts) {
        if (onlyTexts.size() > 1) {
            throw new IllegalArgumentException("{npc-only} est ecrit " + onlyTexts.size() + " fois : une seule fois, "
                + "avant le premier [NEXT ACT] (pour faire arriver un personnage plus tard, utilisez {npc-arrives}).");
        }
        return onlyTexts.isEmpty() ? StartingRoster.EVERYBODY : new StartingRoster(onlyTexts.get(0));
    }

    /** A "{npc-delta}" in the premise would have nothing to apply to and would reach every character as text. */
    private static void requireNoDelta(String premise) {
        if (!CharacterDeltaMarker.extract(premise).deltas().isEmpty()) {
            throw new IllegalArgumentException("{npc-delta ...} ne peut etre ecrit que dans un acte, pas dans la premisse.");
        }
        if (!StoryDirectiveMarker.extract(premise).directives().isEmpty()) {
            throw new IllegalArgumentException("{npc-story ...} ne peut etre ecrit que dans un acte, pas dans la premisse.");
        }
        if (!ActivityDirectiveMarker.extract(premise).directives().isEmpty()) {
            throw new IllegalArgumentException("{npc-active ...}, {npc-present ...}, {npc-passive ...}, {npc-absent ...}, "
                + "{npc-reset}, {npc-mode ...}, {npc-arrives ...} et {npc-leaves ...} ne peuvent etre ecrits que dans un acte, pas dans la premisse.");
        }
    }

    // -------------------------------------------------------------------------
    // Heading extraction
    // -------------------------------------------------------------------------

    private record Heading(int depth, String title, String body) {}

    private static List<Heading> splitHeadings(String raw) {
        List<Heading> headings = new ArrayList<>();
        String[] lines = raw.split("\n", -1);
        int      i     = 0;
        while (i < lines.length) {
            Matcher m = HEADING.matcher(lines[i]);
            if (!m.matches()) { i++; continue; }
            int    depth = m.group(1).length();
            String title = m.group(2).strip();
            int    start = i + 1;
            int    end   = start;
            while (end < lines.length && !HEADING.matcher(lines[end]).matches()) end++;
            String body = String.join("\n", java.util.Arrays.asList(lines).subList(start, end));
            headings.add(new Heading(depth, title, body));
            i = end;
        }
        return headings;
    }

    private static String textBefore(String raw, Heading first) {
        String[] lines = raw.split("\n", -1);
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            if (HEADING.matcher(line).matches()) break;
            sb.append(line).append('\n');
        }
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Tree building
    // -------------------------------------------------------------------------

    private static final class Node {
        ActNumber  number;
        String     title;
        String     body;        // "{npc-starts}" line already cut out
        List<String> styleNames = new ArrayList<>(); // the "{style name}" lines written under this heading, in order, no repeat
        NpcOpening opening = NpcOpening.NONE;
        List<CharacterDelta> deltas = new ArrayList<>();
        List<ActivityDirective> activity = new ArrayList<>();
        List<StoryDirective> stories = new ArrayList<>();
        List<Node> children = new ArrayList<>();
    }

    private static List<Node> buildTree(List<Heading> headings, List<String> onlyTexts) {
        List<Node> roots = new ArrayList<>();
        List<Node> stack = new ArrayList<>(); // stack.get(i) = open node at depth i+1
        int        rootSiblingIndex = 0;
        for (Heading h : headings) {
            Node node = new Node();
            node.title = h.title;
            SceneStyleMarker.Extracted style = SceneStyleMarker.extract(h.body.strip());
            node.styleNames = style.names().stream().distinct().toList();
            NpcOnlyMarker.Extracted only = NpcOnlyMarker.extract(style.text());
            NpcOpeningMarker.Extracted body = NpcOpeningMarker.extract(only.text());
            CharacterDeltaMarker.Extracted withoutDeltas = CharacterDeltaMarker.extract(body.text());
            StoryDirectiveMarker.Extracted withoutStories = StoryDirectiveMarker.extract(withoutDeltas.text());
            ActivityDirectiveMarker.Extracted withoutActivity = ActivityDirectiveMarker.extract(withoutStories.text());
            node.body     = withoutActivity.text().strip();
            node.opening  = body.opening();
            node.deltas   = withoutDeltas.deltas();
            node.stories  = withoutStories.directives();
            node.activity = withoutActivity.directives();

            int depth = Math.min(h.depth, stack.size() + 1); // tolerate a heading level skip
            while (stack.size() >= depth) stack.remove(stack.size() - 1);

            if (stack.isEmpty()) {
                rootSiblingIndex++;
                node.number = ActNumber.of(rootSiblingIndex);
                roots.add(node);
            } else {
                Node parent = stack.get(stack.size() - 1);
                node.number = parent.number.child(parent.children.size() + 1);
                parent.children.add(node);
            }
            if (!only.namesTexts().isEmpty()) {
                if (!node.number.segments().stream().allMatch(i -> i == 1)) {
                    throw new IllegalArgumentException("{npc-only} dans l'acte " + node.number.display() + " : il doit preceder le "
                        + "premier [NEXT ACT] (en tete du scenario ou dans la premiere scene) ; pour faire arriver un "
                        + "personnage plus tard, utilisez {npc-arrives}.");
                }
                onlyTexts.addAll(only.namesTexts());
            }
            stack.add(node);
        }
        return roots;
    }

    // -------------------------------------------------------------------------
    // Flatten to leaves, resolving inherited text and diffing beats against the previous leaf
    // -------------------------------------------------------------------------

    private static void flatten(Node node, List<Node> path, List<ScenarioAct> leaves, List<Node> previousPath,
                                Function<String, ScenarioStyle> styleReader) {
        path.add(node);
        if (node.children.isEmpty()) {
            String resolvedText = path.stream().map(n -> n.body).filter(b -> !b.isBlank())
                .reduce((a, b) -> a + "\n\n" + b).orElse("");
            List<String> beats = new ArrayList<>();
            NpcOpening opening = NpcOpening.NONE;
            List<CharacterDelta> deltas = new ArrayList<>();
            List<StoryDirective> stories = new ArrayList<>();
            for (Node entered : newlyEntered(path, previousPath)) {
                beats.addAll(Teaser.extractAll(entered.body));
                deltas.addAll(entered.deltas);
                stories.addAll(entered.stories);
                if (opening.isNone()) opening = entered.opening; // le premier marqueur, de la racine a la feuille, decide
            }
            List<List<ActivityDirective>> activityPath = path.stream().map(n -> n.activity).toList();
            leaves.add(new ScenarioAct(node.number, node.title, resolvedText, beats, opening, deltas, activityPath, stories,
                sceneStyleOf(path, styleReader)));
            previousPath.clear();
            previousPath.addAll(path);
        } else {
            for (Node child : node.children) flatten(child, path, leaves, previousPath, styleReader);
        }
        path.remove(path.size() - 1);
    }

    /** The styles of the nearest heading, from the leaf up to the root, that wrote a "{style name}" (added in order) ; NONE if none did. */
    private static ScenarioStyle sceneStyleOf(List<Node> path, Function<String, ScenarioStyle> styleReader) {
        for (int i = path.size() - 1; i >= 0; i--) {
            if (!path.get(i).styleNames.isEmpty()) return styleOf(path.get(i).styleNames, styleReader);
        }
        return ScenarioStyle.NONE;
    }

    /** The styles of these names, one after the other (the first line of a later one comes after the last line of the one before). */
    private static ScenarioStyle styleOf(List<String> names, Function<String, ScenarioStyle> styleReader) {
        ScenarioStyle combined = ScenarioStyle.NONE;
        for (String name : names.stream().distinct().toList()) combined = combined.then(styleReader.apply(name));
        return combined;
    }

    /** Nodes on `path` that were not on `previousPath` at the same position — i.e. newly entered ancestors. */
    private static List<Node> newlyEntered(List<Node> path, List<Node> previousPath) {
        int i = 0;
        while (i < path.size() && i < previousPath.size() && path.get(i) == previousPath.get(i)) i++;
        return path.subList(i, path.size());
    }
}
