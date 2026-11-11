package storymagine.chat.infra;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import storymagine.chat.coeur.domaine.scenario.ChatScenario;
import storymagine.chat.coeur.domaine.validation.ScenarioReport;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The format taught by chat/scenario-builder (English labels, #SCENARIO premise, one step per file, a template
 * arrival, a plan.md at the root) really loads and validates : the playbook must not teach a format the game refuses.
 */
class ScenarioBuilderFormatTest {

    private final ChatFileStorageAdapter adapter = new ChatFileStorageAdapter();

    @Test
    void aScenarioWrittenAsThePlaybookTeachesLoadsAndValidatesWithoutError(@TempDir Path root) throws IOException {
        Path dir = root.resolve("sample");
        Files.createDirectories(dir.resolve("characters"));
        write(dir.resolve("scenario.txt"), """
            Joueur : Sam
            Langue : English

            #SCENARIO
            World: A river town at dusk.
            Premise: Sam arrives in a river town to deliver a sealed letter.
            Tone: Light, with a little tension.
            {npc-only Nell}

            # The arrival
            {style river-mist}
            ## The quay
            [Sam steps off the ferry onto the quay, where Nell is mending a net.]
            Starting point: Nell notices the stranger and the letter in Sam's hand.
            What must happen: Nell asks who the letter is for.
            {20%} A gull screams somewhere above the nets.
            When to write [NEXT ACT]: Sam has said the name written on the letter.
            """);
        write(dir.resolve("scenario-1.txt"), """
            # The market
            {npc-arrives Pip}
            {npc-arrives guard Hobb}
            {npc-delta public Hobb : suspicious of strangers, blocks the market gate}

            ## The gate
            [Sam and Nell reach the market gate, where Pip waves them over and Hobb folds his arms.]
            Starting point: Hobb will not let anyone in without a reason.
            What must happen: Sam finds a way through the gate.
            """);
        write(dir.resolve("characters/nell.txt"), """
            # Nell
            Nell, a net-mender in her forties. Talks while she works, hums when she thinks.

            # SECRET
            Nell wrote the letter herself.
            """);
        write(dir.resolve("characters/pip.txt"), "# Pip\nPip, a quick boy who runs errands. Speaks fast, interrupts himself.\n");
        write(dir.resolve("characters/guard.txt"), "# Guard\n%NPC%, a gate guard. Speaks in short orders and never smiles.\n");
        Files.createDirectories(dir.resolve("styles"));
        write(dir.resolve("styles/river-mist.txt"), "Damp, quiet mood.\n- {20%} Describe the grey light on the water.\n");
        write(dir.resolve("plan.md"), "# Sample\n\n## Brief\n- Audience: general\n");

        ScenarioReport report = adapter.validateScenario(root, "sample");
        ChatScenario scenario = adapter.loadScenario(root, "sample");

        assertFalse(report.hasErrors(), report.toString());
        assertEquals("Sam", scenario.playerName());
        assertEquals(2, scenario.acts().size());
        assertTrue(scenario.styleAt(1).text().contains("Damp, quiet mood."), "the scene style is added");
        assertEquals(Set.of("nell"), scenario.activityAt(1).inSceneIds());
        assertTrue(scenario.activityAt(2).inSceneIds().containsAll(Set.of("nell", "pip", "hobb")));
        assertTrue(scenario.cast().find("guard").isEmpty(), "the template is not an NPC");
    }

    private static void write(Path file, String content) throws IOException {
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }
}
