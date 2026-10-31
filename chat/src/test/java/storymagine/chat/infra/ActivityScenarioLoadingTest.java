package storymagine.chat.infra;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import storymagine.chat.coeur.domaine.activity.ActivitySource;
import storymagine.chat.coeur.domaine.activity.NpcActivity;
import storymagine.chat.coeur.domaine.scenario.ChatScenario;
import storymagine.chat.coeur.domaine.validation.ScenarioValidationException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** How "{npc-active ...}" and friends travel from scenario.txt to the activity of each act. */
class ActivityScenarioLoadingTest {

    private static final String SCENARIO = """
        Monde.

        # Parent
        Texte commun du parent.
        {npc-absent Marcus}

        ## Un
        Corps un.

        ## Deux
        Corps deux.
        {npc-active Marcus}
        {npc-passive}
        """;

    private static void write(Path root, String scenarioText) throws IOException {
        Path dir = root.resolve("inn");
        Files.createDirectories(dir.resolve("characters"));
        Files.writeString(dir.resolve("characters/elena.txt"), "# Elena Voss\nUne marchande.", StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("characters/marcus.txt"), "# Marcus\nUn soldat.", StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("scenario.txt"), scenarioText, StandardCharsets.UTF_8);
    }

    private static ChatScenario load(Path root) {
        return new ChatFileStorageAdapter().loadScenario(root, "inn");
    }

    @Test
    void theCommandLinesNeverReachTheActTextSentToEveryCharacter(@TempDir Path root) throws IOException {
        write(root, SCENARIO);

        for (var act : load(root).acts()) assertFalse(act.text().contains("npc-"), act.text());
    }

    @Test
    void aParentCommandHoldsForItsChildrenUntilTheyOverrideIt(@TempDir Path root) throws IOException {
        write(root, SCENARIO);
        ChatScenario scenario = load(root);

        assertEquals(NpcActivity.ABSENT, scenario.activityAt(1).levelOf("marcus"), "acte 1.1 : herite du parent");
        assertEquals(NpcActivity.ACTIVE, scenario.activityAt(2).levelOf("marcus"), "acte 1.2 : nomme dans le fils");
        assertEquals(ActivitySource.IMPLICIT, scenario.activityAt(2).stateOf("elena").source());
        assertEquals(NpcActivity.PASSIVE, scenario.activityAt(2).levelOf("elena"), "le sans-nom du fils touche Elena");
    }

    @Test
    void aScenarioWithoutAnyCommandKeepsEveryoneAtTheDefault(@TempDir Path root) throws IOException {
        write(root, "Monde.\n\n# Un\nTexte.\n");

        assertEquals(java.util.Set.of("elena", "marcus"), load(root).activityAt(1).inSceneIds());
    }

    @Test
    void aCommandInThePremiseIsRefused(@TempDir Path root) throws IOException {
        write(root, "Monde.\n{npc-absent Marcus}\n\n# Un\nTexte.\n");

        assertThrows(RuntimeException.class, () -> load(root));
    }

    @Test
    void anUnknownCharacterIsReportedWithItsLine(@TempDir Path root) throws IOException {
        write(root, "Monde.\n\n# Un\nTexte.\n{npc-active Zorglub}\n");

        ScenarioValidationException error = assertThrows(ScenarioValidationException.class, () -> load(root));

        assertTrue(error.getMessage().contains("scenario.txt:5") && error.getMessage().contains("Zorglub"), error.getMessage());
    }

    @Test
    void aCommandNotAloneOnItsLineIsReported(@TempDir Path root) throws IOException {
        write(root, "Monde.\n\n# Un\nTexte {npc-active Elena} au milieu.\n");

        ScenarioValidationException error = assertThrows(ScenarioValidationException.class, () -> load(root));

        assertTrue(error.getMessage().contains("scenario.txt:4"), error.getMessage());
    }

    @Test
    void aSceneWhereEveryoneIsAbsentIsRefused(@TempDir Path root) throws IOException {
        write(root, "Monde.\n\n# Un\nTexte.\n{npc-absent}\n");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> load(root));

        assertTrue(error.getMessage().contains("acte 1"), error.getMessage());
    }

    @Test
    void namingTheSameCharacterTwiceInAHeadingIsRefused(@TempDir Path root) throws IOException {
        write(root, "Monde.\n\n# Un\nTexte.\n{npc-active Elena}\n{npc-passive Elena}\n");

        assertThrows(IllegalArgumentException.class, () -> load(root));
    }
}
