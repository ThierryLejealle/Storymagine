package storymagine.chat.coeur.domaine.scenario;

import storymagine.chat.coeur.domaine.activity.ActivityResolver;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Who is in the story when it begins, from the "{npc-only Elena, Marc}" command of the first scene
 * (see NpcOnlyMarker). EVERYBODY (no command) is the classic mode : every character of the Cast is
 * there. With a command, only the named characters are : the others are outside the story (see
 * NpcActivity.OUTSIDE) until an "{npc-arrives}" brings them in. namesText is what the author wrote,
 * resolved against the Cast later because a name may have several words.
 */
public record StartingRoster(String namesText) {

    public static final StartingRoster EVERYBODY = new StartingRoster("");

    public StartingRoster {
        namesText = namesText == null ? "" : namesText.strip();
    }

    public boolean isEverybody() { return namesText.isEmpty(); }

    /**
     * The ids of the Cast members who are NOT in the story at the start.
     * @throws IllegalArgumentException a name that is nobody in the Cast
     */
    public Set<String> outsideIds(Cast cast) {
        Set<String> outside = new LinkedHashSet<>();
        if (isEverybody()) return outside;
        Set<String> named = new LinkedHashSet<>();
        for (Npc npc : ActivityResolver.resolveNames(cast, namesText.replace(',', ' '))) named.add(npc.id());
        for (Npc npc : cast.npcs()) if (!named.contains(npc.id())) outside.add(npc.id());
        return outside;
    }

    /** The named characters, in the order written. */
    public List<Npc> named(Cast cast) {
        return isEverybody() ? cast.npcs() : ActivityResolver.resolveNames(cast, namesText.replace(',', ' '));
    }
}
