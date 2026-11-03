package storymagine.chat.coeur.domaine.pick;

import storymagine.chat.coeur.domaine.scenario.Cast;
import storymagine.chat.coeur.domaine.scenario.Npc;

import java.util.ArrayList;
import java.util.List;

/**
 * The characters a "{cast}" or a "{repeat ... cast}" can take : the ones the author listed (one word
 * each, see Cast.findAllByName), or the whole cast when no list was written. A word that matches no
 * character, matches several, or names a character already listed is an error with a message for the
 * author : the scenario would otherwise draw someone else than meant.
 */
public final class CastPool {

    private CastPool() {}

    public static List<Npc> of(List<String> words, Cast cast) {
        if (cast.npcs().isEmpty()) {
            throw new IllegalArgumentException("aucun personnage dans characters/ : rien a tirer");
        }
        if (words.isEmpty()) return cast.npcs();
        List<Npc> pool = new ArrayList<>();
        for (String word : words) {
            List<Npc> matches = cast.findAllByName(word);
            if (matches.isEmpty()) {
                throw new IllegalArgumentException("aucun personnage '" + word + "' dans characters/");
            }
            if (matches.size() > 1) {
                throw new IllegalArgumentException("'" + word + "' designe plusieurs personnages ("
                    + matches.stream().map(Npc::id).toList() + ") : ecrire l'identifiant de celui voulu");
            }
            Npc npc = matches.get(0);
            if (pool.contains(npc)) {
                throw new IllegalArgumentException("'" + word + "' designe " + npc.id() + ", deja dans la liste");
            }
            pool.add(npc);
        }
        return pool;
    }
}
