package storymagine.chat.infra;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import storymagine.chat.coeur.domaine.scenario.ChatScenario;
import storymagine.chat.coeur.domaine.session.ChatSession;
import storymagine.chat.infra.ScenarioScaffolder.Created;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScenarioScaffolderTest {

    private final ScenarioScaffolder scaffolder = new ScenarioScaffolder();
    private final ChatFileStorageAdapter adapter = new ChatFileStorageAdapter();

    @Test
    void createsTheScenarioFileAndOneCharacterSheetPerNpcAndNothingElse(@TempDir Path root) {
        Created created = scaffolder.create(root, "auberge", List.of("Elena", "Marcus"));

        assertEquals(root.resolve("auberge"), created.scenarioDir());
        assertEquals(List.of(
            root.resolve("auberge/scenario.txt"),
            root.resolve("auberge/characters/elena.txt"),
            root.resolve("auberge/characters/marcus.txt")), created.files());
        assertTrue(created.files().stream().allMatch(Files::isRegularFile));
        assertFalse(Files.exists(root.resolve("auberge/run")), "run/ est cree par le jeu, jamais par le modele");
    }

    @Test
    void theGeneratedScenarioLoadsAsAValidScenarioWithOneActPerSectionAndTheRightCast(@TempDir Path root) {
        scaffolder.create(root, "auberge", List.of("Elena", "Marcus"));

        ChatScenario scenario = adapter.loadScenario(root, "auberge");

        assertEquals("Alex", scenario.playerName());
        assertEquals(2, scenario.acts().size());
        assertEquals(List.of("elena", "marcus"), scenario.cast().npcs().stream().map(n -> n.id()).toList());
        assertEquals("Elena", scenario.cast().find("elena").orElseThrow().name());
    }

    @Test
    void theFirstActNamesEveryNpcSoNoneIsLeftWonderingWhetherItIsInTheRoom(@TempDir Path root) {
        scaffolder.create(root, "auberge", List.of("Elena", "Marcus", "Clara"));

        ChatScenario scenario = adapter.loadScenario(root, "auberge");

        String opening = ChatSession.fresh(scenario).turns().get(0).text();
        assertTrue(opening.contains("Elena, Marcus et Clara sont là."), opening);
    }

    @Test
    void theSeedExplainsNpcStartsWithoutActivatingIt(@TempDir Path root) {
        scaffolder.create(root, "auberge", List.of("Elena", "Marcus"));

        ChatScenario scenario = adapter.loadScenario(root, "auberge");

        assertTrue(scenario.premise().contains("npc-starts"), "la consigne doit etre lisible dans la graine");
        assertTrue(scenario.storyOpening().isNone(), "expliquer le mot-cle ne doit pas le declencher");
    }

    @Test
    void onlyTheFirstActHasANextActConditionBecauseTheLastActHasNothingToAdvanceTo(@TempDir Path root) {
        scaffolder.create(root, "auberge", List.of("Elena"));

        ChatScenario scenario = adapter.loadScenario(root, "auberge");

        assertTrue(scenario.acts().get(0).text().contains("Quand écrire [NEXT ACT]"));
        assertFalse(scenario.acts().get(1).text().contains("[NEXT ACT]"));
    }

    @Test
    void theCharacterSheetHasANameHeadingAndAnOptionalSecretSection(@TempDir Path root) throws IOException {
        scaffolder.create(root, "auberge", List.of("Elena"));

        String sheet = Files.readString(root.resolve("auberge/characters/elena.txt"), StandardCharsets.UTF_8);

        assertTrue(sheet.startsWith("# Elena\n"));
        assertTrue(sheet.contains("\n# SECRET\n"));
        var elena = adapter.loadScenario(root, "auberge").cast().find("elena").orElseThrow();
        assertFalse(elena.secretInfo().isBlank(), "la partie secrete du modele est lue comme telle");
    }

    @Test
    void theFileNameIsDerivedFromTheNpcNameWithoutAccentsOrSpaces(@TempDir Path root) {
        Created created = scaffolder.create(root, "s", List.of("Élise Dupont"));

        assertEquals(root.resolve("s/characters/elise-dupont.txt"), created.files().get(1));
        var npc = adapter.loadScenario(root, "s").cast().find("elise-dupont").orElseThrow();
        assertEquals("Élise Dupont", npc.name(), "le nom affiche garde ses accents");
    }

    @Test
    void twoNpcsWithTheSameFileNameAreToldApart(@TempDir Path root) {
        Created created = scaffolder.create(root, "s", List.of("Elena", "elena", "Éléna"));

        assertEquals(List.of(
            root.resolve("s/characters/elena.txt"),
            root.resolve("s/characters/elena-2.txt"),
            root.resolve("s/characters/elena-3.txt")), created.files().subList(1, 4));
        assertEquals(3, adapter.loadScenario(root, "s").cast().npcs().size());
    }

    @Test
    void aNameWithNoLetterOrDigitFallsBackToAGenericFileName() {
        assertEquals("pnj", ScenarioScaffolder.slug("???"));
        assertEquals("elise-dupont", ScenarioScaffolder.slug("  Élise   Dupont! "));
    }

    @Test
    void presenceSentenceFollowsTheNumberOfNpcs() {
        assertEquals("Elena est là.", ScenarioScaffolder.presenceSentence(List.of("Elena")));
        assertEquals("Elena et Marcus sont là.", ScenarioScaffolder.presenceSentence(List.of("Elena", "Marcus")));
        assertEquals("Elena, Marcus et Clara sont là.", ScenarioScaffolder.presenceSentence(List.of("Elena", "Marcus", "Clara")));
    }

    @Test
    void anExistingScenarioIsNeverOverwritten(@TempDir Path root) throws IOException {
        Files.createDirectories(root.resolve("auberge"));
        Files.writeString(root.resolve("auberge/scenario.txt"), "my precious scenario", StandardCharsets.UTF_8);

        assertThrows(IllegalStateException.class, () -> scaffolder.create(root, "auberge", List.of("Elena")));
        assertEquals("my precious scenario", Files.readString(root.resolve("auberge/scenario.txt"), StandardCharsets.UTF_8));
    }

    @Test
    void anInvalidScenarioNameIsRefusedBeforeAnythingIsWritten(@TempDir Path root) {
        for (String bad : new String[] {"", "mon scenario", "café", "../evil", "a/b"}) {
            assertThrows(IllegalArgumentException.class, () -> scaffolder.create(root, bad, List.of("Elena")), bad);
        }
        assertFalse(ScenarioScaffolder.isValidScenarioName(null));
        assertTrue(ScenarioScaffolder.isValidScenarioName("temple-noir_2"));
    }

    @Test
    void aScenarioNeedsAtLeastOneNpcAndNoBlankName(@TempDir Path root) {
        assertThrows(IllegalArgumentException.class, () -> scaffolder.create(root, "s", List.of()));
        assertThrows(IllegalArgumentException.class, () -> scaffolder.create(root, "s", List.of("Elena", "  ")));
        assertFalse(Files.exists(root.resolve("s")));
    }
}
