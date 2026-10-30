package storymagine.chat.infra;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import storymagine.chat.coeur.domaine.scenario.CharacterDelta;
import storymagine.chat.coeur.domaine.scenario.ChatScenario;
import storymagine.chat.coeur.domaine.scenario.Npc;
import storymagine.chat.coeur.domaine.validation.ScenarioValidationException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** How "{delta-pnj}" travels from scenario.txt to ChatScenario, and what a character's sheet is at each act. */
class CharacterDeltaScenarioLoadingTest {

    private static final String SCENARIO = """
        Monde.

        # Parent
        Texte commun du parent.
        {delta-pnj public Elena : Elle boite depuis l'embuscade.}

        ## Un
        Corps un.
        {delta-pnj secret Marcus : Il a reconnu le chef des bandits.}

        ## Deux
        Corps deux.
        {delta-pnj public Elena : Elle a perdu un oeil.}
        {delta-pnj secret Elena : Elle veut quitter le groupe.}
        """;

    private static void write(Path root, String scenarioText) throws IOException {
        Path dir = root.resolve("inn");
        Files.createDirectories(dir.resolve("characters"));
        Files.writeString(dir.resolve("characters/elena.txt"), "# Elena Voss\nUne marchande.\n# SECRET\nElle cache une dette.", StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("characters/marcus.txt"), "# Marcus\nUn soldat.", StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("scenario.txt"), scenarioText, StandardCharsets.UTF_8);
    }

    private static ChatScenario load(Path root) {
        return new ChatFileStorageAdapter().loadScenario(root, "inn");
    }

    private static Npc npc(ChatScenario scenario, String id) {
        return scenario.cast().find(id).orElseThrow();
    }

    @Test
    void deltaLinesNeverReachTheActTextSentToEveryCharacter(@TempDir Path root) throws IOException {
        write(root, SCENARIO);

        ChatScenario scenario = load(root);

        for (var act : scenario.acts()) {
            assertFalse(act.text().contains("delta-pnj"), act.text());
            assertFalse(act.text().contains("trahir") || act.text().contains("reconnu"), act.text());
        }
        assertTrue(scenario.acts().get(0).text().contains("Texte commun du parent."));
    }

    @Test
    void aParentsDeltaFiresOnceWithTheFirstLeafOfThatParent(@TempDir Path root) throws IOException {
        write(root, SCENARIO);

        ChatScenario scenario = load(root);

        assertEquals(List.of(
            new CharacterDelta(CharacterDelta.Visibility.PUBLIC, "Elena", "Elle boite depuis l'embuscade."),
            new CharacterDelta(CharacterDelta.Visibility.SECRET, "Marcus", "Il a reconnu le chef des bandits.")),
            scenario.acts().get(0).deltas());
        assertEquals(2, scenario.acts().get(1).deltas().size(), "le parent n'est pas rejoue a la feuille suivante");
    }

    @Test
    void sheetsAccumulateActAfterActAndPublicAndSecretGoToTheRightPart(@TempDir Path root) throws IOException {
        write(root, SCENARIO);
        ChatScenario scenario = load(root);
        Npc elena = npc(scenario, "elena");

        Npc atActOne = scenario.sheetAt(elena, 1);
        assertTrue(atActOne.publicInfo().endsWith("Elle boite depuis l'embuscade."));
        assertEquals(elena.secretInfo(), atActOne.secretInfo());

        Npc atActTwo = scenario.sheetAt(elena, 2);
        assertTrue(atActTwo.publicInfo().contains("Elle boite depuis l'embuscade.\nElle a perdu un oeil."), atActTwo.publicInfo());
        assertEquals("Elle cache une dette.\nElle veut quitter le groupe.", atActTwo.secretInfo());
        // La fiche d'origine, elle, n'est jamais modifiee : un retour a l'acte 1 retrouve l'etat de l'acte 1.
        assertEquals(atActOne, scenario.sheetAt(elena, 1));
        assertEquals(elena, scenario.sheetAt(elena, 0));
    }

    @Test
    void aSecretOnACharacterWithoutSecretSectionCreatesIt(@TempDir Path root) throws IOException {
        write(root, SCENARIO);
        ChatScenario scenario = load(root);

        Npc marcus = scenario.sheetAt(npc(scenario, "marcus"), 1);

        assertEquals("Il a reconnu le chef des bandits.", marcus.secretInfo());
        assertEquals(npc(scenario, "marcus").publicInfo(), marcus.publicInfo());
    }

    @Test
    void aDeltaInThePremiseOrOnAnUnknownCharacterFailsAtLoading(@TempDir Path root) throws IOException {
        write(root, "Monde.\n{delta-pnj secret Elena : fuite}\n\n# Un\nCorps.\n");
        assertThrows(IllegalArgumentException.class, () -> load(root));

        write(root, "Monde.\n\n# Un\n{delta-pnj secret Zorglub : x}\nCorps.\n");
        ScenarioValidationException e = assertThrows(ScenarioValidationException.class, () -> load(root));
        assertTrue(e.getMessage().contains("Zorglub"), e.getMessage());
    }
}
