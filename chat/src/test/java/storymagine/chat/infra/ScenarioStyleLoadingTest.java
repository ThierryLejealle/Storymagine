package storymagine.chat.infra;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
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

/** style.txt : optional, read from the scenario directory, shared by every character. */
class ScenarioStyleLoadingTest {

    private static void write(Path root, String fileName, String content) throws IOException {
        Path dir = root.resolve("inn");
        Files.createDirectories(dir.resolve("characters"));
        Files.writeString(dir.resolve("characters/elena.txt"), "# Elena\nUne marchande.", StandardCharsets.UTF_8);
        if (!Files.exists(dir.resolve("scenario.txt"))) {
            Files.writeString(dir.resolve("scenario.txt"), "Elena attend.\n\n# Un\nCorps.\n", StandardCharsets.UTF_8);
        }
        if (fileName != null) Files.writeString(dir.resolve(fileName), content, StandardCharsets.UTF_8);
    }

    private static ChatScenario load(Path root) {
        return new ChatFileStorageAdapter().loadScenario(root, "inn");
    }

    @Test
    void theStyleIsReadAndStripped(@TempDir Path root) throws IOException {
        write(root, "style.txt", "\n  Phrases courtes. Vocabulaire simple.  \n\n");

        ChatScenario scenario = load(root);

        assertTrue(scenario.style().isPresent());
        assertEquals("Phrases courtes. Vocabulaire simple.", scenario.style().text());
    }

    @Test
    void noFileOrABlankFileMeansNoStyle(@TempDir Path root) throws IOException {
        write(root, null, null);
        assertFalse(load(root).style().isPresent());

        write(root, "style.txt", "  \n \n");
        assertFalse(load(root).style().isPresent());
    }

    @Test
    void aStyleFileStartingWithAHeadingIsNotTakenForAnOldCharacterSheet(@TempDir Path root) throws IOException {
        write(root, "style.txt", "# Style\nPhrases courtes.");

        ChatScenario scenario = load(root);

        assertEquals("# Style\nPhrases courtes.", scenario.style().text());
    }

    @Test
    void aCommandInTheStyleIsRefusedInsteadOfBeingSentRawToTheModel(@TempDir Path root) throws IOException {
        write(root, "style.txt", "Ton : {pick random ton}");

        ScenarioValidationException e = assertThrows(ScenarioValidationException.class, () -> load(root));

        assertTrue(e.getMessage().contains("style.txt"), e.getMessage());
    }

    @Test
    void aReloadSeesAnEditedStyle(@TempDir Path root) throws IOException {
        write(root, "style.txt", "Phrases courtes.");
        assertEquals("Phrases courtes.", load(root).style().text());

        Files.writeString(root.resolve("inn/style.txt"), "Phrases longues.", StandardCharsets.UTF_8);

        assertEquals("Phrases longues.", load(root).style().text());
    }
}
