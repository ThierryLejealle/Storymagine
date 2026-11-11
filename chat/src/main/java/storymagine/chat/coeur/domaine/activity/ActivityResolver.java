package storymagine.chat.coeur.domaine.activity;

import storymagine.chat.coeur.domaine.scenario.Cast;
import storymagine.chat.coeur.domaine.scenario.Npc;
import storymagine.chat.coeur.domaine.scenario.NpcOpening;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Computes the CastActivity of an act from the commands written on the path of headings that leads
 * to it (root first), never stored. Per heading : {npc-reset} first, then the commands that name
 * characters (level + explicit), then the one command without a name, which only touches the
 * characters whose level is not explicit yet (level + implicit). The order the lines are written in
 * therefore never matters. A deeper heading overrides a shallower one.
 */
public final class ActivityResolver {

    private ActivityResolver() {}

    /** @throws IllegalArgumentException unknown name, a character named twice or two nameless commands in one heading */
    public static CastActivity resolve(Cast cast, List<List<ActivityDirective>> path) {
        return resolve(cast, path, Set.of());
    }

    /** Same, from a roster where the characters of outsideAtStart are not in the story yet (see StartingRoster). */
    public static CastActivity resolve(Cast cast, List<List<ActivityDirective>> path, Set<String> outsideAtStart) {
        CastActivity activity = CastActivity.defaults(cast, outsideAtStart);
        for (List<ActivityDirective> heading : path) activity = applyHeading(cast, activity, heading);
        return activity;
    }

    private static CastActivity applyHeading(Cast cast, CastActivity before, List<ActivityDirective> heading) {
        CastActivity activity = before;
        for (ActivityDirective directive : heading) {
            if (!directive.isLeaves()) continue;
            for (Npc npc : resolveNames(cast, directive.namesText())) {
                activity = activity.with(npc.id(), new NpcActivityState(NpcActivity.OUTSIDE, ActivitySource.EXPLICIT));
            }
        }
        for (ActivityDirective directive : heading) {
            if (!directive.isArrives()) continue;
            for (Npc npc : arrivalTargets(cast, directive)) {
                if (activity.levelOf(npc.id()).isOutside()) activity = activity.with(npc.id(), NpcActivityState.DEFAULT);
            }
        }
        if (heading.stream().anyMatch(ActivityDirective::isReset)) {
            for (Npc npc : cast.npcs()) {
                if (activity.levelOf(npc.id()).isOutside()) continue;
                activity = activity.with(npc.id(), new NpcActivityState(NpcActivity.PRESENT, ActivitySource.EXPLICIT));
            }
        }
        List<ActivityDirective> all = heading.stream().filter(ActivityDirective::isAll).toList();
        if (all.size() > 1) {
            throw new IllegalArgumentException("Deux commandes * dans le meme titre : "
                + "{npc-" + all.get(0).level().keyword() + " *} et {npc-" + all.get(1).level().keyword() + " *}.");
        }
        if (all.size() == 1) {
            // as if every character were named : explicit, but a name written in the same heading is more specific
            for (Npc npc : cast.npcs()) {
                if (activity.levelOf(npc.id()).isOutside()) continue;
                activity = activity.with(npc.id(), new NpcActivityState(all.get(0).level(), ActivitySource.EXPLICIT));
            }
        }
        Set<String> namedHere = new HashSet<>();
        for (ActivityDirective directive : heading) {
            if (directive.isReset() || directive.isMode() || directive.isUnnamed() || directive.isAll()
                || directive.isArrives() || directive.isLeaves()) continue;
            for (Npc npc : resolveNames(cast, directive.namesText())) {
                if (activity.levelOf(npc.id()).isOutside()) {
                    throw new IllegalArgumentException(npc.label() + " n'est pas dans l'histoire a ce stade : "
                        + "{npc-arrives " + npc.label() + "} doit venir avant {npc-" + directive.level().keyword() + " " + npc.label() + "}.");
                }
                if (!namedHere.add(npc.id())) {
                    throw new IllegalArgumentException(npc.label() + " est nomme deux fois dans le meme titre.");
                }
                activity = activity.with(npc.id(), new NpcActivityState(directive.level(), ActivitySource.EXPLICIT));
            }
        }
        List<ActivityDirective> unnamed = heading.stream().filter(ActivityDirective::isUnnamed).toList();
        if (unnamed.size() > 1) {
            throw new IllegalArgumentException("Deux commandes sans nom dans le meme titre : "
                + "{npc-" + unnamed.get(0).level().keyword() + "} et {npc-" + unnamed.get(1).level().keyword() + "}.");
        }
        if (unnamed.size() == 1) {
            for (Npc npc : cast.npcs()) {
                if (activity.stateOf(npc.id()).source() == ActivitySource.EXPLICIT || activity.levelOf(npc.id()).isOutside()) continue;
                activity = activity.with(npc.id(), new NpcActivityState(unnamed.get(0).level(), ActivitySource.IMPLICIT));
            }
        }
        return activity;
    }

    /** The characters an {npc-arrives ...} brings in : the ones it names, or the ones created from its template. */
    public static List<Npc> arrivalTargets(Cast cast, ActivityDirective directive) {
        ArrivalSpec spec = ArrivalSpec.parse(directive.namesText(), cast);
        if (!spec.isTemplate()) return resolveNames(cast, directive.namesText());
        List<Npc> found = new ArrayList<>();
        for (String name : spec.names()) {
            found.add(cast.findByName(name).orElseThrow(() -> new IllegalArgumentException(
                "aucun personnage de ce nom dans characters/ : " + name)));
        }
        return found;
    }

    /**
     * The characters with `mode` switched on in an act : the {npc-mode ... on|off ...} commands of the
     * path of headings leading to it are replayed root first, in the order written, so a deeper heading
     * (or a later line) overrides an earlier one. Never stored, like the activity.
     * @throws IllegalArgumentException unknown name
     */
    public static Set<String> modeIds(Cast cast, List<List<ActivityDirective>> path, NpcMode mode) {
        Set<String> on = new LinkedHashSet<>();
        for (List<ActivityDirective> heading : path) {
            for (ActivityDirective directive : heading) {
                if (!directive.isMode() || directive.mode() != mode) continue;
                List<String> targets = directive.isModeAll()
                    ? cast.npcs().stream().map(Npc::id).toList()
                    : resolveNames(cast, directive.namesText()).stream().map(Npc::id).toList();
                if (directive.kind() == ActivityDirective.Kind.MODE_ON) on.addAll(targets); else on.removeAll(targets);
            }
        }
        return on;
    }

    /**
     * The characters a named opening ("{npc-starts Elena, Marc}") stands for, in the order written : the
     * first opens, the others follow. A character may be named several times (Elena, Marcus, Elena :
     * they speak three times). Empty for an opening with no name.
     * @throws IllegalArgumentException unknown name
     */
    public static List<Npc> openingNpcs(Cast cast, NpcOpening opening) {
        return opening.isNamed() ? resolveNames(cast, opening.namesText()) : List.of();
    }

    /**
     * The characters a list of names stands for. Names are separated by spaces (or commas) and may have
     * several words ("Celeste Dubois") : at each position the longest run of words that names a character wins.
     */
    public static List<Npc> resolveNames(Cast cast, String namesText) {
        String[] words = namesText.replace(',', ' ').strip().split("\\s+");
        if (words.length > 1 && List.of(words).stream().anyMatch(ActivityDirective.ALL::equals)) {
            throw new IllegalArgumentException("* doit etre seul dans la commande : "
                + "{npc-active *}, ou des noms de personnages, pas les deux.");
        }
        List<Npc> found = new ArrayList<>();
        int i = 0;
        while (i < words.length) {
            Npc match = null;
            int end = words.length;
            for (; end > i; end--) {
                match = cast.findByName(String.join(" ", List.of(words).subList(i, end))).orElse(null);
                if (match != null) break;
            }
            if (match == null) {
                throw new IllegalArgumentException("aucun personnage de ce nom dans characters/ : " + words[i]);
            }
            found.add(match);
            i = end;
        }
        return found;
    }
}
