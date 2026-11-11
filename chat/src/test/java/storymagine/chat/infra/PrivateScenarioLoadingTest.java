package storymagine.chat.infra;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import storymagine.chat.coeur.domaine.scenario.ChatScenario;
import storymagine.chat.coeur.domaine.scenario.Npc;
import storymagine.chat.coeur.domaine.scenario.PrivateLines;
import storymagine.chat.coeur.domaine.validation.ScenarioValidationException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** "{private Name}" in a scenario file : loaded with the scene, read only by the character it names. */
class PrivateScenarioLoadingTest {

    private static final String SCENARIO = """
        Joueur : Alex

        #SCENARIO
        World: un salon.

        # Mario
        Franck et Elena sortent leurs consoles.
        {private Franck} Franck veut gagner et va tricher.
        {private Elena}
        Elena veut juste s'amuser.
        {end-private}
        Quand écrire [NEXT ACT] : Elena a vu la triche.

        # Fin
        Le soir tombe.
        """;

    private static ChatScenario write(Path root, String scenarioText) throws IOException {
        Path dir = root.resolve("salon");
        Files.createDirectories(dir.resolve("characters"));
        Files.writeString(dir.resolve("characters/franck.txt"), "# Franck\nUn joueur.", StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("characters/elena.txt"), "# Elena\nUne amie.", StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("scenario.txt"), scenarioText, StandardCharsets.UTF_8);
        return new ChatFileStorageAdapter().loadScenario(root, "salon");
    }

    @Test
    void eachCharacterReadsTheSceneWithOnlyItsOwnPrivateText(@TempDir Path root) throws IOException {
        ChatScenario scenario = write(root, SCENARIO);
        Npc franck = scenario.cast().findByName("Franck").orElseThrow();
        Npc elena = scenario.cast().findByName("Elena").orElseThrow();
        String scene = scenario.acts().get(0).text();

        String toFranck = PrivateLines.visibleTo(scene, scenario.cast(), franck);
        String toElena = PrivateLines.visibleTo(scene, scenario.cast(), elena);

        assertTrue(toFranck.contains("va tricher") && !toFranck.contains("juste s'amuser"), toFranck);
        assertTrue(toElena.contains("juste s'amuser") && !toElena.contains("tricher"), toElena);
        assertTrue(toFranck.contains("[NEXT ACT]") && toElena.contains("[NEXT ACT]"), "the exit condition stays common");
        assertFalse(toFranck.contains("{") || toElena.contains("{"));
    }

    @Test
    void anUnknownCharacterInAPrivateTextStopsTheLoadingNamingTheScene(@TempDir Path root) {
        String text = SCENARIO.replace("{private Franck}", "{private Zorg}");

        ScenarioValidationException e = assertThrows(ScenarioValidationException.class, () -> write(root, text));

        assertTrue(e.getMessage().contains("Zorg") && e.getMessage().contains("ligne 8"), e.getMessage());
    }

    @Test
    void aBlockLeftOpenStopsTheLoading(@TempDir Path root) {
        String text = SCENARIO.replace("{end-private}\n", "");

        ScenarioValidationException e = assertThrows(ScenarioValidationException.class, () -> write(root, text));

        assertTrue(e.getMessage().contains("private"), e.getMessage());
    }
}
