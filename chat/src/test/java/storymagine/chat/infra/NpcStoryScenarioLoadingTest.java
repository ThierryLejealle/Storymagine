package storymagine.chat.infra;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import storymagine.chat.coeur.domaine.scenario.ChatScenario;
import storymagine.chat.coeur.domaine.scenario.Npc;
import storymagine.chat.coeur.domaine.session.ChatPromptBuilder;
import storymagine.chat.coeur.domaine.session.ChatPromptBuilder.ChatPrompt;
import storymagine.chat.coeur.domaine.session.Scene;
import storymagine.chat.coeur.domaine.validation.ScenarioReport;
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

/** How "## tag" sections and "{npc-story}" travel from the files to ChatScenario, and what each character reads at each act. */
class NpcStoryScenarioLoadingTest {

    private static final String LEA = """
        # Léa
        Léa, vingt-six ans.

        ## froide  distante au début
        Léa est distante avec Marc.

        ## amicale
        Léa est chaleureuse avec Marc.

        # SECRET
        Léa déteste se tromper.

        ## soupcon
        Léa soupçonne Marc d'un trafic.

        ## chatons
        Léa a vu Marc nourrir des chatons, elle le trouve adorable.
        """;

    private static final String SCENARIO = """
        Monde.

        # Le voisin
        {npc-story on Léa froide}
        {npc-story on Léa soupcon}
        [Marc croise Léa.]

        ## Le bonjour
        Ils se saluent.

        ## La filature
        Léa suit Marc.

        # Le soir
        {npc-story off Léa froide}
        {npc-story off Léa soupcon}
        {npc-story on Léa amicale}
        {npc-story on Léa chatons}
        Léa recroise Marc.
        """;

    private static void write(Path root, String scenarioText) throws IOException {
        Path dir = root.resolve("voisin");
        Files.createDirectories(dir.resolve("characters"));
        Files.writeString(dir.resolve("characters/lea.txt"), LEA, StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("characters/marc.txt"), "# Marc\nUn voisin.", StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("scenario.txt"), scenarioText, StandardCharsets.UTF_8);
    }

    private static ChatScenario load(Path root) {
        return new ChatFileStorageAdapter().loadScenario(root, "voisin");
    }

    @Test
    void storyLinesNeverReachTheActTextSentToEveryCharacter(@TempDir Path root) throws IOException {
        write(root, SCENARIO);

        for (var act : load(root).acts()) {
            assertFalse(act.text().contains("npc-story"), act.text());
        }
    }

    @Test
    void aTagStaysOnAcrossLaterActsUntilItIsSwitchedOff(@TempDir Path root) throws IOException {
        write(root, SCENARIO);
        ChatScenario scenario = load(root);
        Npc lea = scenario.cast().find("lea").orElseThrow();

        Npc atFirstAct = scenario.sheetAt(lea, 1);
        assertTrue(atFirstAct.publicInfo().contains("distante avec Marc"), atFirstAct.publicInfo());
        assertTrue(atFirstAct.secretInfo().contains("trafic"), atFirstAct.secretInfo());

        Npc atSecondAct = scenario.sheetAt(lea, 2);
        assertEquals(atFirstAct, atSecondAct, "le titre n'est pas rejoue, l'etat se prolonge");

        Npc atThirdAct = scenario.sheetAt(lea, 3);
        assertFalse(atThirdAct.publicInfo().contains("distante"), atThirdAct.publicInfo());
        assertTrue(atThirdAct.publicInfo().contains("chaleureuse"), atThirdAct.publicInfo());
        assertFalse(atThirdAct.secretInfo().contains("trafic"), atThirdAct.secretInfo());
        assertTrue(atThirdAct.secretInfo().contains("chatons"), atThirdAct.secretInfo());
        assertTrue(atThirdAct.secretInfo().startsWith("Léa déteste se tromper."), "le texte permanent reste");
    }

    @Test
    void goingBackToAnEarlierActGivesBackTheEarlierState(@TempDir Path root) throws IOException {
        write(root, SCENARIO);
        ChatScenario scenario = load(root);
        Npc lea = scenario.cast().find("lea").orElseThrow();

        Npc later = scenario.sheetAt(lea, 3);
        Npc back = scenario.sheetAt(lea, 2);

        assertTrue(back.secretInfo().contains("trafic"));
        assertFalse(back.secretInfo().contains("chatons"));
        assertTrue(later.secretInfo().contains("chatons"));
        assertEquals(lea.publicInfo(), scenario.sheetAt(lea, 0).publicInfo(), "avant tout acte aucun tag n'est actif");
    }

    @Test
    void aSecretTagIsReadByItsOwnerOnlyAndAPublicTagByTheOthersToo(@TempDir Path root) throws IOException {
        write(root, SCENARIO);
        ChatScenario scenario = load(root);
        Npc lea = scenario.cast().find("lea").orElseThrow();
        Npc marc = scenario.cast().find("marc").orElseThrow();

        ChatPrompt leaSpeaks = ChatPromptBuilder.build(scenario, new Scene(lea, List.of(marc), List.of(), false), 3, "", List.of());
        assertTrue(leaSpeaks.system().contains("chaleureuse avec Marc"));
        assertTrue(leaSpeaks.system().contains("adorable"));

        ChatPrompt marcSpeaks = ChatPromptBuilder.build(scenario, new Scene(marc, List.of(lea), List.of(), false), 3, "", List.of());
        assertTrue(marcSpeaks.system().contains("chaleureuse avec Marc"), "un tag public est lu par les autres");
        assertFalse(marcSpeaks.system().contains("adorable"), "un tag secret ne sort jamais de son personnage");
        assertFalse(marcSpeaks.system().contains("##"), marcSpeaks.system());
    }

    @Test
    void aStoryCommandOnAnUnknownTagOrCharacterOrInThePremiseFailsAtLoading(@TempDir Path root) throws IOException {
        write(root, "Monde.\n{npc-story on Léa froide}\n\n# Un\nCorps.\n");
        IllegalArgumentException premise = assertThrows(IllegalArgumentException.class, () -> load(root));
        assertTrue(premise.getMessage().contains("premisse"), premise.getMessage());

        write(root, "Monde.\n\n# Un\n{npc-story on Léa inconnu}\nCorps.\n");
        ScenarioValidationException tag = assertThrows(ScenarioValidationException.class, () -> load(root));
        assertTrue(tag.getMessage().contains("inconnu"), tag.getMessage());

        write(root, "Monde.\n\n# Un\n{npc-story on Zorglub froide}\nCorps.\n");
        ScenarioValidationException npc = assertThrows(ScenarioValidationException.class, () -> load(root));
        assertTrue(npc.getMessage().contains("Zorglub"), npc.getMessage());
    }

    @Test
    void aSheetWithTwoTagsOfTheSameNameIsReportedByTheValidationNotThrown(@TempDir Path root) throws IOException {
        write(root, SCENARIO);
        Files.writeString(root.resolve("voisin/characters/lea.txt"), "# Léa\nTexte.\n## a\none\n## A\ntwo\n", StandardCharsets.UTF_8);

        ScenarioReport report = new ChatFileStorageAdapter().validateScenario(root, "voisin");

        assertEquals(1, report.errors().size(), report.issues().toString());
        assertTrue(report.errors().get(0).message().contains("existe deja"), report.issues().toString());
    }

    @Test
    void aCorrectStoryScenarioValidatesWithoutIssue(@TempDir Path root) throws IOException {
        write(root, SCENARIO);

        ScenarioReport report = new ChatFileStorageAdapter().validateScenario(root, "voisin");

        assertTrue(report.issues().stream().noneMatch(i -> i.message().contains("tag")), report.issues().toString());
        assertTrue(report.errors().isEmpty(), report.issues().toString());
    }
}
