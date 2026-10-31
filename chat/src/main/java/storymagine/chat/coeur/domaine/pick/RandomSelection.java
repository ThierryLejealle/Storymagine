package storymagine.chat.coeur.domaine.pick;

import storymagine.chat.coeur.ports.RandomnessPort;

import java.util.ArrayList;
import java.util.List;

/** Draws different candidates at random : shared by "{pick N ...}" and "{repeat random ...}". */
final class RandomSelection {

    private RandomSelection() {}

    /** count different candidates, each drawn at random among the ones left. */
    static List<Integer> draw(RandomnessPort randomness, List<Integer> candidates, int count) {
        List<Integer> left = new ArrayList<>(candidates);
        List<Integer> chosen = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            chosen.add(left.remove(randomness.nextInt(left.size())));
        }
        return chosen;
    }
}
