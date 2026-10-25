package storymagine.chat.infra;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import storymagine.chat.coeur.domaine.scenario.ChatScenario;
import storymagine.chat.coeur.domaine.session.ChatSession;
import storymagine.chat.coeur.domaine.session.SavePoint;
import storymagine.chat.coeur.ports.RandomnessPort;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** [PICK: ...] resolution as the storage adapter drives it : once per run, kept by saves, redrawn by a new run. */
class ChatFileStoragePicksTest {

    /** Always draws the same index, changeable between two loads. */
    private static final class FixedRandomness implements RandomnessPort {
        int index;
        @Override public int nextInt(int bound) { return index; }
    }

    private final FixedRandomness randomness = new FixedRandomness();
    private final ChatFileStorageAdapter adapter = new ChatFileStorageAdapter(randomness);

    @Test
    void firstLoadResolvesTheTagsAndWritesTheResolvedCopyWhileTheSourceStaysUntouched(@TempDir Path root) throws IOException {
        writeScenario(root, "Alex arrives. [PICK: place]");
        randomness.index = 1;

        ChatScenario scenario = adapter.loadScenario(root, "inn");

        assertTrue(scenario.premise().contains("Night market."), scenario.premise());
        assertFalse(scenario.premise().contains("PICK"));
        assertTrue(Files.readString(root.resolve("inn/scenario.txt")).contains("[PICK: place]"));
        assertTrue(Files.readString(root.resolve("inn/run/current/resolved/scenario.txt")).contains("Night market."));
    }

    @Test
    void laterLoadsOfTheSameRunKeepTheSameDraws(@TempDir Path root) throws IOException {
        writeScenario(root, "Alex arrives. [PICK: place]");
        randomness.index = 1;
        adapter.loadScenario(root, "inn");

        randomness.index = 2;
        ChatScenario again = adapter.loadScenario(root, "inn");

        assertTrue(again.premise().contains("Night market."), again.premise());
    }

    @Test
    void resettingTheRunMakesTheNextLoadDrawAgain(@TempDir Path root) throws IOException {
        writeScenario(root, "Alex arrives. [PICK: place]");
        randomness.index = 1;
        ChatScenario first = adapter.loadScenario(root, "inn");

        adapter.resetSession(root, first);
        randomness.index = 2;
        ChatScenario second = adapter.loadScenario(root, "inn");

        assertTrue(second.premise().contains("Empty town."), second.premise());
    }

    @Test
    void aSavePointCarriesItsDrawsAndLoadingItBringsThemBack(@TempDir Path root) throws IOException {
        writeScenario(root, "Alex arrives. [PICK: place]");
        randomness.index = 0;
        ChatScenario first = adapter.loadScenario(root, "inn");
        ChatSession session = ChatSession.fresh(first);
        adapter.saveSession(root, session);
        SavePoint save = adapter.createSavePoint(root, session);

        adapter.resetSession(root, first);
        randomness.index = 2;
        adapter.loadScenario(root, "inn"); // the new run draws "Empty town."

        ChatSession restored = adapter.loadSavePoint(root, first, save.id());

        assertTrue(restored.scenario().premise().contains("Foggy harbor."), restored.scenario().premise());
        assertTrue(adapter.loadScenario(root, "inn").premise().contains("Foggy harbor."));
    }

    @Test
    void picksAlsoResolveInContinuationFilesWithPickUniqueSharingTheMemoryAcrossFiles(@TempDir Path root) throws IOException {
        writeScenario(root, "Alex arrives. [PICK-UNIQUE: place]");
        Files.writeString(root.resolve("inn/scenario-1.txt"), "# Act two\n[PICK-UNIQUE: place]\n", StandardCharsets.UTF_8);
        randomness.index = 0;

        ChatScenario scenario = adapter.loadScenario(root, "inn");

        String all = scenario.premise() + scenario.acts();
        assertTrue(all.contains("Foggy harbor."), all);
        assertTrue(all.contains("Night market."), all);
    }

    @Test
    void aScenarioWithoutTagsLeavesNoResolvedCopyAndNoRunDirectory(@TempDir Path root) throws IOException {
        writeScenario(root, "Alex arrives. Nothing random here.");

        adapter.loadScenario(root, "inn");

        assertFalse(Files.exists(root.resolve("inn/run")));
    }

    @Test
    void aMissingPicksFileFailsTheLoadWithAMessageNamingTheTag(@TempDir Path root) throws IOException {
        writeScenario(root, "Alex arrives. [PICK: weather]");

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> adapter.loadScenario(root, "inn"));

        assertTrue(e.getMessage().contains("weather"), e.getMessage());
        assertFalse(Files.exists(root.resolve("inn/run/current/resolved")), "nothing half-written");
    }

    private static void writeScenario(Path root, String scenarioText) throws IOException {
        Path dir = root.resolve("inn");
        Files.createDirectories(dir.resolve("characters"));
        Files.createDirectories(dir.resolve("picks"));
        Files.writeString(dir.resolve("characters/elena.txt"), "# Elena\nA barmaid.", StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("scenario.txt"), "#SCENARIO\n" + scenarioText + "\n", StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("picks/place.txt"), "Foggy harbor.\n---\nNight market.\n---\nEmpty town.\n", StandardCharsets.UTF_8);
    }
}
