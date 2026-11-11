package storymagine.chat.infra;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import storymagine.chat.coeur.domaine.pick.PickParagraphs;
import storymagine.chat.coeur.domaine.pick.PickResolver;
import storymagine.chat.coeur.domaine.scenario.ChanceLines;
import storymagine.chat.coeur.domaine.scenario.ChatScenario;
import storymagine.chat.coeur.domaine.scenario.ScenarioStyle;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** "{style name}" under a heading (a style for a scene and its sub-scenes) and "{N%}" lines in scenario texts. */
class SceneStyleTest {

    private static final String SCENARIO = """
        Joueur : Alex

        #SCENARIO
        Monde : un village.

        # Auberge
        {style auberge}
        Texte de l'auberge.

        ## Salle
        Texte de la salle.

        ## Cave
        {style cave-humide}
        Texte de la cave.

        # Foret
        Texte de la foret.
        """;

    private static ChatScenario write(Path root, String scenarioText) throws IOException {
        writeFiles(root, scenarioText);
        return new ChatFileStorageAdapter().loadScenario(root, "inn");
    }

    private static void writeFiles(Path root, String scenarioText) throws IOException {
        Path dir = root.resolve("inn");
        Files.createDirectories(dir.resolve("characters"));
        Files.createDirectories(dir.resolve("styles"));
        Files.writeString(dir.resolve("characters/elena.txt"), "# Elena\nUne marchande.", StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("style.txt"), "Phrases courtes.", StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("styles/auberge.txt"), "Ambiance chaude.", StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("styles/cave-humide.txt"), "Ambiance froide.", StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("scenario.txt"), scenarioText, StandardCharsets.UTF_8);
    }

    @Test
    void aSceneStyleIsAddedAfterTheGlobalOneAndHoldsForTheSubScenes(@TempDir Path root) throws IOException {
        ChatScenario scenario = write(root, SCENARIO);

        assertEquals("Phrases courtes.\nAmbiance chaude.", scenario.styleAt(1).text(), "Salle inherits Auberge");
        assertEquals("Phrases courtes.\nAmbiance froide.", scenario.styleAt(2).text(), "Cave's own style replaces its parent's");
        assertEquals("Phrases courtes.", scenario.styleAt(3).text(), "Foret has none : only style.txt");
        assertEquals("Phrases courtes.", scenario.styleAt(0).text(), "no act in use : only style.txt");
    }

    @Test
    void theStyleCommandNeverReachesTheTextSentToTheCharacters(@TempDir Path root) throws IOException {
        for (var act : write(root, SCENARIO).acts()) assertFalse(act.text().contains("{style"), act.text());
    }

    @Test
    void aMissingStyleFileIsAnErrorNamingTheFile(@TempDir Path root) throws IOException {
        String text = SCENARIO.replace("{style auberge}", "{style inconnu}");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> write(root, text));

        assertTrue(e.getMessage().contains("styles/inconnu.txt"), e.getMessage());
    }

    @Test
    void aStyleCommandNotAloneOnItsLineIsRefused() {
        assertThrows(IllegalArgumentException.class,
            () -> ScenarioOutlineParser.parse("Monde.\n\n# Acte\nTexte {style a} au milieu.", name -> ScenarioStyle.of(name)));
        assertThrows(IllegalArgumentException.class,
            () -> ScenarioOutlineParser.parse("Monde {style a} ici.\n\n# Acte\nTexte.", name -> ScenarioStyle.of(name)));
    }

    @Test
    void severalStylesUnderOneHeadingAddUpInTheOrderWrittenAndARepeatedNameCountsOnce() {
        var outline = ScenarioOutlineParser.parse("Monde.\n\n# Acte\n{style a}\n{style b}\n{style a}\nTexte.", name -> ScenarioStyle.of(name));

        assertEquals("a\nb", outline.acts().get(0).sceneStyle().text());
        assertEquals("Texte.", outline.acts().get(0).text());
    }

    @Test
    void aStyleInThePremiseIsAddedToTheGlobalStyleForTheWholeStory(@TempDir Path root) throws IOException {
        ChatScenario scenario = write(root, SCENARIO.replace("Monde : un village.", "Monde : un village.\n{style auberge}"));

        assertEquals("Phrases courtes.\nAmbiance chaude.", scenario.style().text());
        assertEquals("Phrases courtes.\nAmbiance chaude.\nAmbiance froide.", scenario.styleAt(2).text(), "premise first, then the scene's");
        assertFalse(scenario.premise().contains("{style"), scenario.premise());
    }

    @Test
    void aStyleInAScenarioWithoutHeadingsIsAddedToTheGlobalStyle() {
        var outline = ScenarioOutlineParser.parse("Monde.\n{style auberge}\nSuite.", name -> ScenarioStyle.of(name));

        assertEquals("Monde.\nSuite.", outline.premise());
        assertEquals("auberge", outline.premiseStyle().text());
    }

    @Test
    void theValidationReportsAMalformedStyleCommandAndAMissingFile(@TempDir Path root) throws IOException {
        write(root, SCENARIO);
        Files.writeString(root.resolve("inn/scenario.txt"), SCENARIO.replace("{style auberge}", "{style ../x}"), StandardCharsets.UTF_8);

        var report = new ChatFileStorageAdapter().validateScenario(root, "inn");

        assertTrue(report.hasErrors(), report.issues().toString());
    }

    @Test
    void aChanceLineIsKeptWithoutItsMarkerOrDroppedWholeAndOtherLinesAreUntouched() {
        String text = "Avant.\n{30%} Un menestrel joue.\nApres.\n\nFin.";

        assertEquals("Avant.\nUn menestrel joue.\nApres.\n\nFin.", ChanceLines.draw(text, bound -> 0), "0 < 30 : kept");
        assertEquals("Avant.\nApres.\n\nFin.", ChanceLines.draw(text, bound -> 99), "99 >= 30 : dropped, line break included");
        assertEquals("sans marqueur", ChanceLines.draw("sans marqueur", bound -> 99));
    }

    @Test
    void aChanceLineIsDrawnOnceWhenTheGameStartsAndFrozenWithTheResolvedFiles(@TempDir Path root) throws IOException {
        String text = SCENARIO.replace("Texte de la foret.", "Texte de la foret.\n{50%} Un corbeau croasse.");
        Path dir = root.resolve("inn");
        writeFiles(root, text); // nothing is loaded yet : no draw is frozen before ours

        assertTrue(PickResolver.containsTag(text), "a chance marker makes the scenario one that is resolved at the start");
        ChatScenario kept = new ChatFileStorageAdapter(bound -> 0).loadScenario(root, "inn");
        assertTrue(kept.acts().get(2).text().contains("Un corbeau croasse."), kept.acts().get(2).text());
        assertFalse(kept.acts().get(2).text().contains("{50%}"));

        // The run is now frozen (resolved/ written): another adapter that would draw 'dropped' reads the same result.
        ChatScenario again = new ChatFileStorageAdapter(bound -> 99).loadScenario(root, "inn");
        assertEquals(kept.acts().get(2).text(), again.acts().get(2).text());
        assertTrue(Files.isDirectory(dir));
    }

    @Test
    void aChanceLineThatIsDroppedTakesItsCommandsWithIt() {
        List<String> resolved = new PickResolver(tag -> PickParagraphs.parse("x"), bound -> 99)
            .resolveAll(List.of("{10%} {pick random lieu}\nResté."));

        assertEquals("Resté.", resolved.get(0));
    }
}
