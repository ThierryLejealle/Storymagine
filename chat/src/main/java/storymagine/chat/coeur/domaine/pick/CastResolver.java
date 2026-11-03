package storymagine.chat.coeur.domaine.pick;

import storymagine.chat.coeur.domaine.scenario.Cast;
import storymagine.chat.coeur.domaine.scenario.Npc;
import storymagine.chat.coeur.ports.RandomnessPort;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/**
 * Runs the "{cast ...}" commands of a scenario's texts (see CastCommand) and replaces every "%NAME%"
 * with the character the variable holds at that point. The commands are taken in the order of the
 * texts ; the characters are searched all together, with backtracking, so a draw that leaves a later
 * command with nobody is undone and another is tried : a scenario fails only when no combination
 * exists at all (or when there are too many to try, see MAX_NODES). Which characters are possible for a
 * command: its list (or the whole cast), minus the ones the other variables hold at that moment unless
 * "reuse-possible", minus the ones this variable already had when "unique".
 * A "{repeat}" copy that holds a cast is wrapped by RepeatExpander in two internal lines, copy-start and
 * copy-end : the variables declared inside a copy are forgotten at its end, the ones declared before stay.
 * "{repeat ... cast NAME}" binds NAME at the start of each copy with a cast-bind line ; that variable counts
 * as held but is not itself constrained (the repeat goes through the whole list).
 * The command lines and these internal lines are removed from the text. Pure : same texts, same draws,
 * same result.
 */
public final class CastResolver {

    static final String COPY_START = "{copy-start}";
    static final String COPY_END = "{copy-end}";
    private static final Pattern BIND = Pattern.compile("^\\{cast-bind\\s+(\\S+)\\s+(\\S+)\\s*}$");
    private static final Pattern USE = Pattern.compile("%([A-Za-z][A-Za-z0-9_]*)%");
    /** A search that needs more steps than this is given up : the author is asked to simplify. */
    private static final int MAX_NODES = 200_000;

    private final Cast cast;
    private final RandomnessPort randomness;

    public CastResolver(Cast cast, RandomnessPort randomness) {
        this.cast = cast;
        this.randomness = randomness;
    }

    /** The texts without command lines and with their variables replaced, plus one draw per cast command. */
    public record Resolution(List<String> texts, List<PickDraw> draws) {}

    static String bindLine(String variable, String npcId) {
        return "{cast-bind " + variable + " " + npcId + "}";
    }

    /** True if the pass has anything to do : a "{cast" or the internal lines of a copy. */
    public static boolean isNeeded(String text) {
        return CastCommand.appearsIn(text) || text.contains(COPY_START);
    }

    public Resolution resolve(List<String> texts) {
        List<Item> items = parse(texts);
        Search search = new Search(items);
        if (!search.solve(0, State.EMPTY)) {
            Item failed = items.get(search.deepest);
            throw new IllegalStateException(failed.command.source() + " : aucune combinaison possible, plus aucun personnage disponible"
                + " pour cette commande (liste, exclusivite des autres variables, unique ou nombre de copies du repeat)");
        }
        return substitute(items, search.assigned, texts.size());
    }

    // ── Reading the texts ───────────────────────────────────────────────────

    private enum Kind { TEXT, START, END, BIND, CAST }

    private record Item(Kind kind, int textIndex, String text, String variable, String npcId, CastCommand command, List<Npc> pool) {}

    private List<Item> parse(List<String> texts) {
        List<Item> items = new ArrayList<>();
        for (int t = 0; t < texts.size(); t++) {
            for (String line : texts.get(t).split("\n", -1)) {
                String stripped = line.strip();
                Matcher bind = BIND.matcher(stripped);
                if (stripped.equals(COPY_START)) {
                    items.add(new Item(Kind.START, t, line, null, null, null, null));
                } else if (stripped.equals(COPY_END)) {
                    items.add(new Item(Kind.END, t, line, null, null, null, null));
                } else if (bind.matches()) {
                    items.add(new Item(Kind.BIND, t, line, bind.group(1), bind.group(2), null, null));
                } else {
                    Optional<CastCommand> command = CastCommand.parseLine(line);
                    if (command.isPresent()) {
                        items.add(new Item(Kind.CAST, t, line, command.get().variable(), null, command.get(), poolOf(command.get())));
                    } else {
                        items.add(new Item(Kind.TEXT, t, line, null, null, null, null));
                    }
                }
            }
        }
        return items;
    }

    private List<Npc> poolOf(CastCommand command) {
        try {
            return CastPool.of(command.candidates(), cast);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(command.source() + " : " + e.getMessage());
        }
    }

    // ── What the variables hold ─────────────────────────────────────────────

    /** The variables in force (innermost copy last) and the characters each variable has had. Never modified. */
    private record State(List<Map<String, String>> scopes, Map<String, Set<String>> history) {

        static final State EMPTY = new State(List.of(Map.of()), Map.of());

        State push() {
            List<Map<String, String>> next = new ArrayList<>(scopes);
            next.add(Map.of());
            return new State(next, history);
        }

        State pop() {
            if (scopes.size() == 1) return this;
            return new State(new ArrayList<>(scopes.subList(0, scopes.size() - 1)), history);
        }

        State set(String variable, String npcId) {
            List<Map<String, String>> next = new ArrayList<>(scopes);
            Map<String, String> innermost = new HashMap<>(next.get(next.size() - 1));
            innermost.put(key(variable), npcId);
            next.set(next.size() - 1, innermost);
            return new State(next, history);
        }

        State remember(String variable, String npcId) {
            Map<String, Set<String>> next = new HashMap<>(history);
            Set<String> had = new HashSet<>(next.getOrDefault(key(variable), Set.of()));
            had.add(npcId);
            next.put(key(variable), had);
            return new State(scopes, next);
        }

        Optional<String> get(String variable) {
            for (int i = scopes.size() - 1; i >= 0; i--) {
                String value = scopes.get(i).get(key(variable));
                if (value != null) return Optional.of(value);
            }
            return Optional.empty();
        }

        /** The characters held by the variables other than `variable`, the innermost value of each. */
        Set<String> heldExcept(String variable) {
            Map<String, String> current = new HashMap<>();
            for (Map<String, String> scope : scopes) current.putAll(scope);
            current.remove(key(variable));
            return new HashSet<>(current.values());
        }

        Set<String> had(String variable) {
            return history.getOrDefault(key(variable), Set.of());
        }

        private static String key(String variable) {
            return variable.toLowerCase(Locale.ROOT);
        }
    }

    // ── The search ──────────────────────────────────────────────────────────

    private final class Search {

        private final List<Item> items;
        final String[] assigned;
        int deepest = 0;
        private int nodes = 0;

        Search(List<Item> items) {
            this.items = items;
            this.assigned = new String[items.size()];
        }

        boolean solve(int from, State state) {
            if (++nodes > MAX_NODES) {
                throw new IllegalStateException("trop de combinaisons a essayer pour placer les {cast} de ce scenario :"
                    + " reduire le nombre de variables, de copies ou de contraintes (unique)");
            }
            State current = state;
            for (int i = from; i < items.size(); i++) {
                Item item = items.get(i);
                switch (item.kind) {
                    case START -> current = current.push();
                    case END -> current = current.pop();
                    case BIND -> current = current.set(item.variable, item.npcId);
                    case CAST -> {
                        List<Npc> candidates = candidatesFor(item, current);
                        for (int index : shuffled(candidates.size())) {
                            String id = candidates.get(index).id();
                            assigned[i] = id;
                            if (solve(i + 1, current.set(item.variable, id).remember(item.variable, id))) return true;
                        }
                        deepest = Math.max(deepest, i);
                        return false;
                    }
                    case TEXT -> { }
                }
            }
            return true;
        }

        private List<Npc> candidatesFor(Item item, State state) {
            Set<String> held = item.command.reusePossible() ? Set.of() : state.heldExcept(item.variable);
            Set<String> had = item.command.unique() ? state.had(item.variable) : Set.of();
            return item.pool.stream().filter(npc -> !held.contains(npc.id()) && !had.contains(npc.id())).toList();
        }

        private List<Integer> shuffled(int size) {
            return RandomSelection.draw(randomness, IntStream.range(0, size).boxed().toList(), size);
        }
    }

    // ── Writing the result ──────────────────────────────────────────────────

    private Resolution substitute(List<Item> items, String[] assigned, int textCount) {
        List<List<String>> lines = new ArrayList<>();
        for (int t = 0; t < textCount; t++) lines.add(new ArrayList<>());
        List<PickDraw> draws = new ArrayList<>();
        State state = State.EMPTY;
        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            switch (item.kind) {
                case START -> state = state.push();
                case END -> state = state.pop();
                case BIND -> state = state.set(item.variable, item.npcId);
                case CAST -> {
                    state = state.set(item.variable, assigned[i]);
                    draws.add(new PickDraw(PickMode.CAST, new PickTag(item.variable), List.of(labelOf(assigned[i]))));
                }
                case TEXT -> lines.get(item.textIndex).add(replaceVariables(item.text, state));
            }
        }
        return new Resolution(lines.stream().map(l -> String.join("\n", l)).toList(), draws);
    }

    private String replaceVariables(String line, State state) {
        return USE.matcher(line).replaceAll(match -> {
            String id = state.get(match.group(1)).orElseThrow(() -> new IllegalArgumentException(
                "%" + match.group(1) + "% n'est pas declaree avant cette ligne (cast NOM) : " + line.strip()));
            return Matcher.quoteReplacement(labelOf(id));
        });
    }

    private String labelOf(String npcId) {
        return cast.find(npcId).map(Npc::label).orElse(npcId);
    }
}
