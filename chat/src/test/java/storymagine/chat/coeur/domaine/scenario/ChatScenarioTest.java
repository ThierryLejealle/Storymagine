package storymagine.chat.coeur.domaine.scenario;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatScenarioTest {

    private static final Cast EMPTY_CAST = new Cast(List.of());

    @Test
    void fourArgConstructorDefaultsPlayerNameToAlex() {
        ChatScenario scenario = new ChatScenario("test", EMPTY_CAST, "premise", List.of());

        assertEquals("Alex", scenario.playerName());
    }

    @Test
    void nullPlayerNameNormalizesToAlex() {
        ChatScenario scenario = new ChatScenario("test", EMPTY_CAST, "premise", List.of(), null);

        assertEquals("Alex", scenario.playerName());
    }

    @Test
    void blankPlayerNameNormalizesToAlex() {
        ChatScenario scenario = new ChatScenario("test", EMPTY_CAST, "premise", List.of(), "   ");

        assertEquals("Alex", scenario.playerName());
    }

    @Test
    void explicitPlayerNameIsKeptAsIs() {
        ChatScenario scenario = new ChatScenario("test", EMPTY_CAST, "premise", List.of(), "Kael");

        assertEquals("Kael", scenario.playerName());
    }
}
