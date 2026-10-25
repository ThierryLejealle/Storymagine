package storymagine.chat.infra;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import storymagine.chat.coeur.domaine.scenario.ChatScenario;
import storymagine.chat.coeur.domaine.scenario.Teaser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** How "[NPC STARTS]" travels from scenario.txt to ChatScenario : story level, act level, errors. */
class NpcOpeningScenarioLoadingTest {

    private static void write(Path root, String scenarioText) throws IOException {
        Path dir = root.resolve("inn");
        Files.createDirectories(dir.resolve("characters"));
        Files.writeString(dir.resolve("characters/elena.txt"), "# Elena Voss\nA merchant.", StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("characters/marcus.txt"), "# Marcus\nA soldier.", StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("scenario.txt"), scenarioText, StandardCharsets.UTF_8);
    }

    private static ChatScenario load(Path root) {
        return new ChatFileStorageAdapter().loadScenario(root, "inn");
    }

    @Test
    void storyLevelMarkerIsReadAndCutOutOfThePremise(@TempDir Path root) throws IOException {
        write(root, "Joueur : Alex\n\n#SCENARIO\nMonde : un chateau.\n[NPC STARTS: elena]\nTon : sombre.\n\n# Acte un\nCorps.\n");

        ChatScenario scenario = load(root);

        assertEquals("elena", scenario.storyOpening().npcName());
        assertFalse(scenario.premise().contains("NPC STARTS"));
        assertTrue(scenario.openingOfAct(1).isNamed(), "l'ouverture de l'histoire est aussi celle du 1er acte");
        assertTrue(scenario.openingOfAct(2).isNone());
    }

    @Test
    void actLevelMarkerBelongsToThatActOnlyAndIsNotATeaserBeat(@TempDir Path root) throws IOException {
        write(root, "Monde.\n\n# Un\nCorps un.\n\n# Deux\n[NPC STARTS: Marcus]\n[Un bruit dans la nuit.]\nCorps deux.\n");

        ChatScenario scenario = load(root);

        assertTrue(scenario.storyOpening().isNone());
        assertTrue(scenario.openingOfAct(1).isNone());
        assertEquals("Marcus", scenario.openingOfAct(2).npcName());
        assertFalse(scenario.acts().get(1).text().contains("NPC STARTS"));
        assertEquals(java.util.List.of("Un bruit dans la nuit."), scenario.acts().get(1).beats());
        assertEquals(java.util.List.of("Un bruit dans la nuit."), Teaser.extractAll(scenario.acts().get(1).text()));
    }

    @Test
    void aMarkerOnAParentActIsInheritedByTheFirstLeafOnly(@TempDir Path root) throws IOException {
        write(root, "Monde.\n\n# Parent\n[NPC STARTS]\nCorps parent.\n\n## Feuille A\nA.\n\n## Feuille B\nB.\n");

        ChatScenario scenario = load(root);

        assertTrue(scenario.acts().get(0).opening().npcStarts());
        assertTrue(scenario.acts().get(1).opening().isNone(), "le parent n'est plus 'nouvellement entre' a la feuille suivante");
    }

    @Test
    void anUnknownNpcNameFailsLoudlyAtLoading(@TempDir Path root) throws IOException {
        write(root, "Monde.\n[NPC STARTS: Zorglub]\n");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> load(root));

        assertTrue(e.getMessage().contains("Zorglub"));
    }

    @Test
    void aScenarioWithoutMarkerHasNoOpening(@TempDir Path root) throws IOException {
        write(root, "Monde.\n\n# Un\nCorps.\n");

        ChatScenario scenario = load(root);

        assertTrue(scenario.storyOpening().isNone());
        assertTrue(scenario.openingOfAct(1).isNone());
    }
}
