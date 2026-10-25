package storymagine.chat.infra;

import storymagine.chat.coeur.ports.RandomnessPort;

import java.util.Random;

/** RandomnessPort backed by java.util.Random. */
public class JavaRandomnessAdapter implements RandomnessPort {

    private final Random random = new Random();

    @Override
    public int nextInt(int bound) {
        return random.nextInt(bound);
    }
}
