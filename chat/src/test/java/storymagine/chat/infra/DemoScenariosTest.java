package storymagine.chat.infra;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import storymagine.chat.coeur.domaine.scenario.ChatScenario;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards chatscenarios/demos (one tiny scenario per mechanism) from rotting : every demo must validate
 * without error, and load with the number of acts and characters its README promises. Works on a copy,
 * so loading never creates a run/ directory in the repository. No LLM involved.
 */
class DemoScenariosTest {

    private static final Path DEMOS = Path.of("..", "chatscenarios", "demos");

    /** demo name -> acts, characters. */
    private static final Map<String, int[]> EXPECTED = Map.ofEntries(
        Map.entry("demo-pick",            new int[]{1, 1}),
        Map.entry("demo-repeat",          new int[]{5, 1}),   // intro, 3 tasks drawn from the file, pause
        Map.entry("demo-actes",           new int[]{3, 1}),
        Map.entry("demo-cast",            new int[]{4, 3}),   // 3 recettes (une par personnage) puis le dessert
        Map.entry("demo-lib",             new int[]{1, 1}),   // lit ../lib/evenements.txt
        Map.entry("demo-multi-fichiers",  new int[]{3, 1}),
        Map.entry("demo-pnj",             new int[]{1, 2}),
        Map.entry("demo-npc-starts",      new int[]{2, 1}),
        Map.entry("demo-npc-do",          new int[]{3, 1}),
        Map.entry("demo-npc-delta",       new int[]{2, 2}),
        Map.entry("demo-npc-story",       new int[]{3, 1}),
        Map.entry("demo-secret",          new int[]{1, 1}),
        Map.entry("demo-activite",        new int[]{2, 3}),
        Map.entry("demo-arrivals",        new int[]{4, 4}),   // Elena, Orlo et les deux gobelins crees depuis le modele (le modele n'en est pas un)
        Map.entry("demo-scene-style",     new int[]{2, 2}),  // un style par scene
        Map.entry("demo-style",           new int[]{1, 1}));

    private final ChatFileStorageAdapter adapter = new ChatFileStorageAdapter(new java.util.Random(1)::nextInt);

    @Test
    void everyMechanismHasItsDemoAndNothingElseIsListed() {
        assertEquals(EXPECTED.keySet().stream().sorted().toList(), adapter.listScenarioNames(DEMOS));
    }

    @Test
    void everyDemoValidatesWithoutErrorOrWarning() {
        for (String name : adapter.listScenarioNames(DEMOS)) {
            var report = adapter.validateScenario(DEMOS, name);
            assertTrue(report.issues().isEmpty(), name + " : " + report.issues());
        }
    }

    @Test
    void everyDemoLoadsWithTheActsAndCharactersItPromises(@TempDir Path copy) throws IOException {
        copyTree(DEMOS, copy);
        for (var expected : EXPECTED.entrySet()) {
            ChatScenario scenario = adapter.loadScenario(copy, expected.getKey());
            assertEquals(expected.getValue()[0], scenario.acts().size(), expected.getKey() + " : actes");
            assertEquals(expected.getValue()[1], scenario.cast().npcs().size(), expected.getKey() + " : personnages");
        }
    }

    private static void copyTree(Path from, Path to) throws IOException {
        List<Path> paths;
        try (Stream<Path> walk = Files.walk(from)) {
            paths = walk.toList();
        }
        for (Path path : paths) {
            if (path.toString().contains(java.io.File.separator + "run")) continue;
            Path target = to.resolve(from.relativize(path).toString());
            if (Files.isDirectory(path)) Files.createDirectories(target);
            else Files.copy(path, target);
        }
    }
}
