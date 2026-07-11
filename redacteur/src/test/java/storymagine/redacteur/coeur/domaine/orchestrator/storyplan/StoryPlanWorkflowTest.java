package storymagine.redacteur.coeur.domaine.orchestrator.storyplan;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import storymagine.redacteur.RedacteurModule;
import storymagine.redacteur.coeur.domaine.orchestrator.CapturingLogPort;
import storymagine.redacteur.coeur.domaine.orchestrator.GenerationConfig;
import storymagine.redacteur.coeur.domaine.orchestrator.MockModelCallPort;
import storymagine.redacteur.infra.scenario.ScenarioFileAdapter;

import java.net.URISyntaxException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests StoryPlanWorkflow (whole-book planning + StoryFidelityCritic/StoryNarrativeCritic/
 * StoryCausalCritic) via the full RedacteurModule wiring, using mock LLMs — no real Ollama
 * calls. Fixture "as-du-ciel" has 8 chapters.
 *
 * Note: FULL's book-level thresholds are deliberately very lenient (bookAverageThreshold=1.0,
 * bookEliminationThreshold=0.0) while these three agents are not yet well tested. Since
 * CriticOutputParser.calculateScore() never returns below 1.0 (even for an all-DEFAUT_MAJEUR
 * response), the retry path is intentionally unreachable today — see
 * bookCritiqueThresholdsAreLenient_evenMajorDefectsDoNotTriggerRetry below, which proves this
 * directly rather than exercising a rejeu that cannot currently happen.
 */
class StoryPlanWorkflowTest {

    private static Path scenarioRoot;
    private static final int CHAPTER_COUNT = 8;

    /** Worst realistic tiered-critic response: floors calculateScore() at exactly 1.0. */
    private static final String WORST_CASE_CRITIQUE = """
            AMELIORATION: [RIEN]
            DEFAUT_SIGNIFICATIF: [RIEN]
            DEFAUT_MAJEUR: probleme 1
            DEFAUT_MAJEUR: probleme 2
            DEFAUT_MAJEUR: probleme 3
            DEFAUT_MAJEUR: probleme 4
            DEFAUT_MAJEUR: probleme 5
            DEFAUT_MAJEUR: probleme 6
            DEFAUT_MAJEUR: probleme 7
            DEFAUT_MAJEUR: probleme 8
            DEFAUT_MAJEUR: probleme 9
            DEFAUT_MAJEUR: probleme 10
            """;

    @BeforeAll
    static void findScenarioRoot() throws URISyntaxException {
        var url = StoryPlanWorkflowTest.class.getClassLoader().getResource("scenarios/as-du-ciel");
        assertNotNull(url, "Test fixture scenarios/as-du-ciel not found in test resources");
        scenarioRoot = Path.of(url.toURI());
    }

    @Test
    void bookCritiquePasses_plansEveryChapterOnce() {
        CapturingLogPort log = new CapturingLogPort();
        var mock = MockModelCallPort.builder(32768)
            .when("editeur narratif",                      MockModelCallPort.CRITIC_PASS)
            .when("verificateur de coherence",             MockModelCallPort.CRITIC_PASS)
            .when("fidelite des PLANS de chapitre",        MockModelCallPort.CRITIC_PASS)
            .when("PLAN DE CHAPITRE remplit son objectif", MockModelCallPort.SCORE_PASS)
            .when("archiviste d'un roman",                 "Resume du chapitre.")
            .otherwise(MockModelCallPort.UNIVERSAL_PASS)
            .build();

        RedacteurModule.assemble(mock, new ScenarioFileAdapter(), log)
            .generate(scenarioRoot, GenerationConfig.full());

        assertTrue(log.hasPhase("PLAN LIVRE"),           "Expected PLAN LIVRE phase header");
        assertTrue(log.hasCritic("StoryFidelityCritic"),  "Expected StoryFidelityCritic logged");
        assertTrue(log.hasCritic("StoryNarrativeCritic"), "Expected StoryNarrativeCritic logged");
        assertTrue(log.hasCritic("StoryCausalCritic"),    "Expected StoryCausalCritic logged");
        assertEquals(1, log.countPhase("PLAN LIVRE"),
            "A single passing attempt should not retry the book plan");
        assertEquals(CHAPTER_COUNT, chapterPlannerCalls(log),
            "Every chapter should be planned exactly once when the book critique passes immediately");
    }

    @Test
    void bookCritiqueThresholdsAreLenient_evenMajorDefectsDoNotTriggerRetry() {
        CapturingLogPort log = new CapturingLogPort();
        var mock = MockModelCallPort.builder(32768)
            // More specific book-level fragments checked first so they win over the chapter-level
            // ones below, which share overlapping wording ("editeur narratif" / "verificateur de
            // coherence") but must stay clean so ChapterPlanWorkflow's own retry isn't triggered.
            .when("PLANS de TOUS les chapitres",           WORST_CASE_CRITIQUE)
            .when("coherence causale",                     WORST_CASE_CRITIQUE)
            .when("fidelite des PLANS de chapitre",        WORST_CASE_CRITIQUE)
            .when("editeur narratif",                      MockModelCallPort.CRITIC_PASS)
            .when("verificateur de coherence",              MockModelCallPort.CRITIC_PASS)
            .when("PLAN DE CHAPITRE remplit son objectif", MockModelCallPort.SCORE_PASS)
            .when("archiviste d'un roman",                 "Resume du chapitre.")
            .otherwise(MockModelCallPort.UNIVERSAL_PASS)
            .build();

        RedacteurModule.assemble(mock, new ScenarioFileAdapter(), log)
            .generate(scenarioRoot, GenerationConfig.full());

        assertEquals(1, log.countPhase("PLAN LIVRE"),
            "Even an all-DEFAUT_MAJEUR response from all 3 book critics must not retry, "
            + "given the intentionally lenient FULL thresholds (avg/elim = 1.0/0.0)");
        assertTrue(log.hasCritic("StoryFidelityCritic"));
        assertTrue(log.hasCritic("StoryNarrativeCritic"));
        assertTrue(log.hasCritic("StoryCausalCritic"));
    }

    @Test
    void runsBookCriticsFalse_skipsGlobalCritiqueButStillPlansChapters() {
        CapturingLogPort log = new CapturingLogPort();
        var mock = MockModelCallPort.builder(32768)
            .when("editeur narratif",                      MockModelCallPort.CRITIC_PASS)
            .when("verificateur de coherence",             MockModelCallPort.CRITIC_PASS)
            .when("PLAN DE CHAPITRE remplit son objectif", MockModelCallPort.SCORE_PASS)
            .when("archiviste d'un roman",                 "Resume du chapitre.")
            .otherwise(MockModelCallPort.UNIVERSAL_PASS)
            .build();

        RedacteurModule.assemble(mock, new ScenarioFileAdapter(), log)
            .generate(scenarioRoot, GenerationConfig.simple());

        assertFalse(log.hasCritic("StoryFidelityCritic"),
            "SIMPLE level must not run the book-level critique");
        assertFalse(log.hasCritic("StoryNarrativeCritic"),
            "SIMPLE level must not run the book-level critique");
        assertFalse(log.hasCritic("StoryCausalCritic"),
            "SIMPLE level must not run the book-level critique");
        assertEquals(1, log.countPhase("PLAN LIVRE"),
            "No retry possible when the book critique is skipped entirely");
        assertEquals(CHAPTER_COUNT, chapterPlannerCalls(log),
            "Chapters are still planned once even when the book critique is disabled");
    }

    /** Number of ChapterPlanner invocations logged — one per chapter per book-planning attempt. */
    private static long chapterPlannerCalls(CapturingLogPort log) {
        return log.steps.stream().filter(s -> s.equals("ChapterPlanner")).count();
    }
}
