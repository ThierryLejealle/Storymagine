package storymagine.chat.infra;

import storymagine.chat.coeur.domaine.activity.ForcedActivity;
import storymagine.chat.coeur.domaine.activity.NpcActivity;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The few settings of a run that cannot be deduced from its turns : the act in force right now
 * (it can change without any turn, via the manual "previous act" button) and the activity levels the
 * player forced by clicking a character (see ForcedActivity). Text format of state.txt, one
 * "key=value" per line :
 * <pre>
 * act=2
 * forced=elena:active@1.2;marcus:absent@
 * </pre>
 * Each forced entry is "id:level@anchor", the anchor being the heading it hangs from (empty = the
 * root). A missing or blank key parses to null — the caller falls back to its default (nothing
 * forced). present / interjecting are the keys of the files written before the activity levels : they
 * are still read (see ChatFileStorageAdapter) but never written again.
 */
record RunState(Integer act, Set<String> present, Set<String> interjecting, Map<String, ForcedActivity> forced) {

    String format() {
        StringBuilder sb = new StringBuilder();
        if (act != null) sb.append("act=").append(act).append('\n');
        if (forced != null && !forced.isEmpty()) {
            sb.append("forced=").append(forced.entrySet().stream()
                .map(e -> e.getKey() + ":" + e.getValue().level().keyword() + "@" + e.getValue().anchor())
                .collect(Collectors.joining(";"))).append('\n');
        }
        return sb.toString();
    }

    static RunState parse(String content) {
        Integer act = null;
        Set<String> present = null;
        Set<String> interjecting = null;
        Map<String, ForcedActivity> forced = null;
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
                case "forced"       -> forced = forcedEntries(value);
                default             -> { }
            }
        }
        return new RunState(act, present, interjecting, forced);
    }

    private static Map<String, ForcedActivity> forcedEntries(String value) {
        Map<String, ForcedActivity> forced = new LinkedHashMap<>();
        for (String entry : value.split(";")) {
            int colon = entry.indexOf(':');
            int at = entry.indexOf('@');
            if (colon <= 0 || at < colon) continue;
            NpcActivity.fromKeyword(entry.substring(colon + 1, at).strip()).ifPresent(level ->
                forced.put(entry.substring(0, colon).strip(), new ForcedActivity(level, entry.substring(at + 1).strip())));
        }
        return forced;
    }

    private static Set<String> ids(String value) {
        return Arrays.stream(value.split(","))
            .map(String::strip)
            .filter(id -> !id.isEmpty())
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
