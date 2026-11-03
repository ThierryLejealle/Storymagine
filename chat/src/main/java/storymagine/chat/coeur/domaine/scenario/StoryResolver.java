package storymagine.chat.coeur.domaine.scenario;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Which "## tag" sections of a character's sheet are in force at a given act : the "{npc-story}"
 * commands of acts 1..N replayed in order, each one switching a tag on or off for the rest of the story.
 * Computed on demand from the acts, never stored, so rolling back, going to the previous act or loading
 * a save need no special handling (same principle as ChatScenario.sheetAt for the deltas).
 */
public final class StoryResolver {

    private StoryResolver() {}

    /** Lowercase names of the tags of `npc` that are on at act `act` (1-based ; 0 = no acts in use). */
    public static Set<String> activeTags(Cast cast, List<ScenarioAct> acts, int act, Npc npc) {
        Set<String> active = new HashSet<>();
        for (int i = 0; i < Math.min(act, acts.size()); i++) {
            for (StoryDirective directive : acts.get(i).stories()) {
                if (!targets(cast, directive, npc)) continue;
                String tag = directive.tag().toLowerCase(Locale.ROOT);
                if (directive.on()) active.add(tag); else active.remove(tag);
            }
        }
        return active;
    }

    /** True if the command names this character (by id, name or a word of the name, see Cast.findByName). */
    public static boolean targets(Cast cast, StoryDirective directive, Npc npc) {
        return cast.findByName(directive.npcName()).map(n -> n.id().equals(npc.id())).orElse(false);
    }
}
