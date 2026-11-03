package storymagine.chat.infra;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import storymagine.chat.coeur.domaine.scenario.ChatScenario;
import storymagine.chat.coeur.domaine.validation.ScenarioReport;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** "/lib/name" : a pick library shared by the scenarios of one directory, next to them. */
class SharedLibraryTest {

    private static void file(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    private static void scenario(Path root, String name, String text) throws IOException {
        file(root.resolve(name + "/characters/elena.txt"), "# Elena\nUne marchande.");
        file(root.resolve(name + "/scenario.txt"), text);
    }

    private static ChatFileStorageAdapter adapter() {
        return new ChatFileStorageAdapter(new Random(5)::nextInt);
    }

    @Test
    void aSharedLibraryIsReadFromTheLibDirectoryNextToTheScenarios(@TempDir Path root) throws IOException {
        file(root.resolve("lib/evenements.txt"), "Un orage eclate.\n---\nUn cirque arrive.");
        scenario(root, "histoire", "Monde. {pick random /lib/evenements}\n\n# Un\nElena entre.\n");

        ChatScenario loaded = adapter().loadScenario(root, "histoire");

        assertTrue(loaded.premise().contains("Un orage eclate.") || loaded.premise().contains("Un cirque arrive."), loaded.premise());
        assertFalse(loaded.premise().contains("/lib/"), loaded.premise());
    }

    @Test
    void severalScenariosShareTheSameLibraryAndSubfoldersAreAllowed(@TempDir Path root) throws IOException {
        file(root.resolve("lib/lieux/ville.txt"), "Le port.");
        scenario(root, "premiere", "Monde. {pick all /lib/lieux/ville}\n\n# Un\nElena entre.\n");
        scenario(root, "seconde", "Autre monde. {pick all /lib/lieux/ville}\n\n# Un\nElena entre.\n");

        assertTrue(adapter().loadScenario(root, "premiere").premise().contains("Le port."));
        assertTrue(adapter().loadScenario(root, "seconde").premise().contains("Le port."));
    }

    @Test
    void repeatCanReadASharedLibraryToo(@TempDir Path root) throws IOException {
        file(root.resolve("lib/defis.txt"), "Pont.\n---\nEnigme.\n---\nDuel.");
        scenario(root, "histoire", "Monde.\n\n{repeat order /lib/defis}\n# Defi {n}\nElena releve : {item}\n{end-repeat}\n");

        ChatScenario loaded = adapter().loadScenario(root, "histoire");

        assertEquals(3, loaded.acts().size());
        assertTrue(loaded.acts().get(1).text().contains("Enigme."));
    }

    @Test
    void theLibDirectoryIsNotAScenarioAndAMissingLibraryIsAnError(@TempDir Path root) throws IOException {
        file(root.resolve("lib/evenements.txt"), "Un orage eclate.");
        scenario(root, "histoire", "Monde. {pick random /lib/absente}\n\n# Un\nElena entre.\n");

        assertEquals(List.of("histoire"), adapter().listScenarioNames(root));
        ScenarioReport report = adapter().validateScenario(root, "histoire");
        assertEquals(1, report.errors().size(), report.issues().toString());
        assertTrue(report.errors().get(0).message().contains("lib/absente.txt"), report.errors().get(0).message());
    }

    @Test
    void uniqueCountsWhatASharedLibraryAlreadyGaveAndNamesTheFileWhenItRunsOut(@TempDir Path root) throws IOException {
        file(root.resolve("lib/evenements.txt"), "Un orage eclate.\n---\nUn cirque arrive.");
        scenario(root, "histoire", "Monde. {pick unique /lib/evenements} {pick unique /lib/evenements} {pick unique /lib/evenements}\n\n# Un\nElena entre.\n");

        ScenarioReport report = adapter().validateScenario(root, "histoire");

        assertEquals(1, report.errors().size(), report.issues().toString());
        assertTrue(report.errors().get(0).message().contains("lib/evenements.txt"), report.errors().get(0).message());
    }

    @Test
    void theDrawIsFrozenForTheRunEvenIfTheLibraryChangesAfterwards(@TempDir Path root) throws IOException {
        file(root.resolve("lib/evenements.txt"), "Un orage eclate.");
        scenario(root, "histoire", "Monde. {pick random /lib/evenements}\n\n# Un\nElena entre.\n");
        ChatScenario first = adapter().loadScenario(root, "histoire");

        file(root.resolve("lib/evenements.txt"), "Une greve eclate.");
        ChatScenario again = adapter().loadScenario(root, "histoire");

        assertEquals(first.premise(), again.premise());
        assertTrue(again.premise().contains("Un orage eclate."));
    }

    @Test
    void theDrawsTableShowsTheSharedLibraryByItsPath(@TempDir Path root) throws IOException {
        file(root.resolve("lib/evenements.txt"), "Un orage eclate.");
        scenario(root, "histoire", "Monde. {pick random /lib/evenements}\n\n# Un\nElena entre.\n");
        ChatScenario loaded = adapter().loadScenario(root, "histoire");

        var draws = adapter().pickDraws(root, loaded).values();

        assertEquals(1, draws.size());
        assertEquals("/lib/evenements", draws.get(0).tag().name());
    }
}
