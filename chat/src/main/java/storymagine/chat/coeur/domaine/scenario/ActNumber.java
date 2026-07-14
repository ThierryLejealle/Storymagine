package storymagine.chat.coeur.domaine.scenario;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Dotted outline numbering for a ScenarioAct (1, 1.1, 1.1.1...), deduced from a node's position
 * among its siblings and its depth in the act tree — authors never type numbers themselves.
 * Natural ordering follows outline convention : a prefix always sorts before its children
 * (1 &lt; 1.1 &lt; 1.1.1 &lt; 1.2 &lt; 2).
 */
public record ActNumber(List<Integer> segments) implements Comparable<ActNumber> {

    public static ActNumber of(int... segments) {
        List<Integer> list = new ArrayList<>();
        for (int s : segments) list.add(s);
        return new ActNumber(list);
    }

    /** The number of this node's Nth child (1-based). */
    public ActNumber child(int childIndex) {
        List<Integer> next = new ArrayList<>(segments);
        next.add(childIndex);
        return new ActNumber(next);
    }

    public String display() {
        return segments.stream().map(String::valueOf).collect(Collectors.joining("."));
    }

    @Override
    public int compareTo(ActNumber other) {
        int len = Math.min(segments.size(), other.segments.size());
        for (int i = 0; i < len; i++) {
            int c = Integer.compare(segments.get(i), other.segments.get(i));
            if (c != 0) return c;
        }
        return Integer.compare(segments.size(), other.segments.size());
    }

    @Override
    public String toString() {
        return display();
    }
}
