package storymagine.chat.coeur.ports;

/**
 * Where the core gets its randomness from (a PICK draw, see PickResolver) — a port so that a test
 * can plug a scripted sequence in and get the same scenario every time.
 */
public interface RandomnessPort {

    /** A value in [0, bound), bound > 0. */
    int nextInt(int bound);
}
