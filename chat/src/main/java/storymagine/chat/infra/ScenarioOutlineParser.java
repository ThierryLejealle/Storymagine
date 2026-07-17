package storymagine.chat.infra;

import storymagine.chat.coeur.domaine.scenario.ActNumber;
import storymagine.chat.coeur.domaine.scenario.ScenarioAct;
import storymagine.chat.coeur.domaine.scenario.Teaser;

import java.util.ArrayList;
import java.util.List;
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

    record Outline(String premise, List<ScenarioAct> acts) {}

    static Outline parse(String rawInput) {
        // Normalise CRLF/CR -> LF avant tout decoupage sur "\n" : sans ca, chaque ligne garde un
        // '\r' final invisible, et HEADING.matches() echoue TOUJOURS dessus — '.' n'inclut pas '\r'
        // par defaut en Java, donc "(.*)$" ne peut jamais l'absorber. Resultat observe en vrai : un
        // fichier scenario.txt enregistre avec des fins de ligne Windows (CRLF) perdait tous ses
        // titres "#"/"##", scenario.acts() se retrouvait vide, et le fallback "pas d'actes" (voir
        // ChatSession.fresh) deversait toute la premisse d'un coup — voir evols/2026-07-17-...
        String raw = rawInput.replace("\r\n", "\n").replace("\r", "\n");
        List<Heading> headings = splitHeadings(raw);
        if (headings.isEmpty()) return new Outline(raw.strip(), List.of());

        Heading first = headings.get(0);
        String premise;
        List<Heading> actHeadings;
        if (first.depth == 1 && first.title.strip().equalsIgnoreCase(SCENARIO_MARKER)) {
            premise = first.body.strip();
            actHeadings = headings.subList(1, headings.size());
        } else {
            premise = textBefore(raw, first).strip();
            actHeadings = headings;
        }

        List<Node> roots = buildTree(actHeadings);
        List<ScenarioAct> leaves = new ArrayList<>();
        for (Node root : roots) flatten(root, new ArrayList<>(), leaves, new ArrayList<>());
        return new Outline(premise, leaves);
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
        String     body;
        List<Node> children = new ArrayList<>();
    }

    private static List<Node> buildTree(List<Heading> headings) {
        List<Node> roots = new ArrayList<>();
        List<Node> stack = new ArrayList<>(); // stack.get(i) = open node at depth i+1
        int        rootSiblingIndex = 0;
        for (Heading h : headings) {
            Node node = new Node();
            node.title = h.title;
            node.body  = h.body.strip();

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
            stack.add(node);
        }
        return roots;
    }

    // -------------------------------------------------------------------------
    // Flatten to leaves, resolving inherited text and diffing beats against the previous leaf
    // -------------------------------------------------------------------------

    private static void flatten(Node node, List<Node> path, List<ScenarioAct> leaves, List<Node> previousPath) {
        path.add(node);
        if (node.children.isEmpty()) {
            String resolvedText = path.stream().map(n -> n.body).filter(b -> !b.isBlank())
                .reduce((a, b) -> a + "\n\n" + b).orElse("");
            List<String> beats = new ArrayList<>();
            for (String own : newlyEntered(path, previousPath).stream().map(n -> n.body).toList()) {
                beats.addAll(Teaser.extractAll(own));
            }
            leaves.add(new ScenarioAct(node.number, node.title, resolvedText, beats));
            previousPath.clear();
            previousPath.addAll(path);
        } else {
            for (Node child : node.children) flatten(child, path, leaves, previousPath);
        }
        path.remove(path.size() - 1);
    }

    /** Nodes on `path` that were not on `previousPath` at the same position — i.e. newly entered ancestors. */
    private static List<Node> newlyEntered(List<Node> path, List<Node> previousPath) {
        int i = 0;
        while (i < path.size() && i < previousPath.size() && path.get(i) == previousPath.get(i)) i++;
        return path.subList(i, path.size());
    }
}
