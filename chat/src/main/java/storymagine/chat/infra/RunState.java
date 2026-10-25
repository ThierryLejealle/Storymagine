package storymagine.chat.infra;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The few settings of a run that cannot be deduced from its turns : the act in force right now
 * (it can change without any turn, via the manual "previous act" button), which Npcs are present
 * and which may interject. Text format of state.txt, one "key=value" per line :
 * <pre>
 * act=2
 * present=elena,marcus
 * interjecting=elena
 * </pre>
 * A missing or blank key parses to null — the caller falls back to its default (every Npc present
 * and eligible, like a fresh run).
 */
record RunState(Integer act, Set<String> present, Set<String> interjecting) {

    String format() {
        StringBuilder sb = new StringBuilder();
        if (act != null) sb.append("act=").append(act).append('\n');
        if (present != null) sb.append("present=").append(String.join(",", present)).append('\n');
        if (interjecting != null) sb.append("interjecting=").append(String.join(",", interjecting)).append('\n');
        return sb.toString();
    }

    static RunState parse(String content) {
        Integer act = null;
        Set<String> present = null;
        Set<String> interjecting = null;
        for (String line : content.split("\n")) {
            int eq = line.indexOf('=');
            if (eq < 0) continue;
            String key = line.substring(0, eq).strip();
            String value = line.substring(eq + 1).strip();
            if (value.isEmpty()) continue;
            switch (key) {
                case "act"          -> act = Integer.valueOf(value);
                case "present"      -> present = ids(value);
                case "interjecting" -> interjecting = ids(value);
                default             -> { }
            }
        }
        return new RunState(act, present, interjecting);
    }

    private static Set<String> ids(String value) {
        return Arrays.stream(value.split(","))
            .map(String::strip)
            .filter(id -> !id.isEmpty())
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
