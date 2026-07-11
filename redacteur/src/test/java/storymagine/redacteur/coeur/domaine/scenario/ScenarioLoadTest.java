package storymagine.redacteur.coeur.domaine.scenario;

import org.junit.jupiter.api.Test;
import storymagine.redacteur.infra.scenario.ScenarioFileAdapter;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test — loads the as-du-ciel test fixture from src/test/resources.
 * The fixture is a stable copy; changes to scenarios/ do not affect this test.
 */
class ScenarioLoadTest {

    private final ScenarioFileAdapter adapter = new ScenarioFileAdapter();

    private Path scenarioRoot() throws URISyntaxException {
        var url = getClass().getClassLoader().getResource("scenarios/as-du-ciel");
        assertNotNull(url, "Test fixture scenarios/as-du-ciel not found in test resources");
        return Path.of(url.toURI());
    }

    @Test
    void validate_returnsNoErrors() throws URISyntaxException {
        var errors = adapter.validate(scenarioRoot());
        assertEquals(List.of(), errors, "Unexpected validation errors: " + errors);
    }

    @Test
    void load_scenarioConfig() throws URISyntaxException {
        Scenario s = adapter.load(scenarioRoot());
        assertEquals("L'As du Ciel", s.config().title());
        assertEquals("fr", s.config().language());
        assertTrue(s.config().contextWindow() > 0);
    }

    @Test
    void load_focusPool() throws URISyntaxException {
        Scenario s = adapter.load(scenarioRoot());
        assertFalse(s.focus().isEmpty());
        assertTrue(s.focus().find("CIEL").isPresent(), "Expected [CIEL] focus element");
        assertTrue(s.focus().find("MACHINE").isPresent());
    }

    @Test
    void load_lorePool() throws URISyntaxException {
        Scenario s = adapter.load(scenarioRoot());
        assertFalse(s.lore().isEmpty());
        assertTrue(s.lore().find("MERLIN").isPresent(), "Expected [MERLIN] lore element");
    }

    @Test
    void load_personnages() throws URISyntaxException {
        Scenario s = adapter.load(scenarioRoot());
        assertFalse(s.personnages().all().isEmpty());
        assertTrue(s.personnages().find("pierre_moreau").isPresent());
        var pierre = s.personnages().find("pierre_moreau").get();
        assertNotNull(pierre.globalContent(), "Pierre should have general content");
        assertNotNull(pierre.writerContent(), "Pierre should have description content");
    }

    @Test
    void load_chapters() throws URISyntaxException {
        Scenario s = adapter.load(scenarioRoot());
        assertFalse(s.chapters().isEmpty());
        Chapter chap1 = s.chapters().get(0);
        assertEquals(NarrativeType.IMPERATIVE, chap1.type());
        assertFalse(chap1.sequences().isEmpty());
        assertFalse(chap1.defaults().focus().isEmpty(), "Chapter 1 should have default focus");
    }

    @Test
    void load_globalRequirements() throws URISyntaxException {
        Scenario s = adapter.load(scenarioRoot());
        assertFalse(s.requirements().planRequirements().isEmpty());
        assertFalse(s.requirements().writerRequirements().isEmpty());
    }
}
