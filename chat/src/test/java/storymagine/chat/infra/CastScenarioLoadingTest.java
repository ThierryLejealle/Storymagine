package storymagine.chat.infra;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import storymagine.chat.coeur.domaine.pick.PickDraw;
import storymagine.chat.coeur.domaine.pick.PickDraws;
import storymagine.chat.coeur.domaine.pick.PickMode;
import storymagine.chat.coeur.domaine.scenario.ChatScenario;
import storymagine.chat.coeur.domaine.scenario.ScenarioAct;
import storymagine.chat.coeur.domaine.validation.ScenarioReport;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** "{cast}" from the files of a scenario to the loaded acts : the chosen characters reach commands and texts, and stay the same for the whole run. */
class CastScenarioLoadingTest {

    private static final String SCENARIO = """
        Monde.

        # Les recettes
        {repeat order cast CHEF}
        ## Recette {n}
        {cast COMMIS}
        {npc-starts %CHEF%}
        [Chef : %CHEF%. Commis : %COMMIS%.]
        Quand écrire [NEXT ACT] : Fin.
        {end-repeat}
        """;

    private static void write(Path root, String scenarioText) throws IOException {
        Path dir = root.resolve("atelier");
        Files.createDirectories(dir.resolve("characters"));
        for (String name : List.of("Jack", "Mia", "John")) {
            Files.writeString(dir.resolve("characters/" + name.toLowerCase() + ".txt"), "# " + name + "\nUn participant.", StandardCharsets.UTF_8);
        }
        Files.writeString(dir.resolve("scenario.txt"), scenarioText, StandardCharsets.UTF_8);
    }

    private static ChatFileStorageAdapter adapter(long seed) {
        return new ChatFileStorageAdapter(new Random(seed)::nextInt);
    }

    @Test
    void theChiefOpensEachRecipeAndTheAssistantIsAnotherCharacter(@TempDir Path root) throws IOException {
        for (long seed = 0; seed < 10; seed++) {
            write(root, SCENARIO);
            ChatScenario scenario = adapter(seed).loadScenario(root, "atelier");

            assertEquals(3, scenario.acts().size());
            for (ScenarioAct act : scenario.acts()) {
                String beat = act.beats().get(0);
                Matcher names = Pattern.compile("Chef : (\\w+)\\. Commis : (\\w+)\\.").matcher(beat);
                assertTrue(names.find(), beat);
                assertEquals(names.group(1), act.opening().namesText(), "graine " + seed + " : " + beat);
                assertFalse(names.group(1).equals(names.group(2)), "graine " + seed + " : " + beat);
            }
            deleteRun(root);
        }
    }

    @Test
    void theDrawsAreRecordedAndFrozenForTheRun(@TempDir Path root) throws IOException {
        write(root, SCENARIO);
        ChatScenario first = adapter(1).loadScenario(root, "atelier");
        ChatScenario again = adapter(99).loadScenario(root, "atelier");

        assertEquals(first.acts().get(0).beats(), again.acts().get(0).beats(), "un run deja tire garde ses personnages");
        PickDraws draws = adapter(1).pickDraws(root, first);
        assertEquals(3, draws.values().stream().filter(d -> d.mode().equals(PickMode.REPEAT_ORDER)).count(), "un par copie");
        List<PickDraw> casts = draws.values().stream().filter(d -> d.mode().equals(PickMode.CAST)).toList();
        assertEquals(3, casts.size());
        assertEquals(Set.of("COMMIS"), Set.copyOf(casts.stream().map(d -> d.tag().name()).toList()));
    }

    @Test
    void validationDoesNotWriteARunAndFindsNoIssue(@TempDir Path root) throws IOException {
        write(root, SCENARIO);

        ScenarioReport report = adapter(1).validateScenario(root, "atelier");

        assertTrue(report.errors().isEmpty(), report.issues().toString());
        assertFalse(Files.exists(root.resolve("atelier/run")), "valider ne cree pas de run");
    }

    private static void deleteRun(Path root) throws IOException {
        Path run = root.resolve("atelier/run");
        if (!Files.exists(run)) return;
        try (var walk = Files.walk(run)) {
            for (Path p : walk.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(p);
        }
    }
}
