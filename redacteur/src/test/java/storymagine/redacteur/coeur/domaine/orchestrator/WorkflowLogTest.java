package storymagine.redacteur.coeur.domaine.orchestrator;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import storymagine.redacteur.RedacteurModule;
import storymagine.redacteur.infra.scenario.ScenarioFileAdapter;

import java.net.URISyntaxException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the log integration of PlanWorkflow, WriteWorkflow, and EvaluateWorkflow
 * using a mock LLM — no real Ollama calls.
 */
class WorkflowLogTest {

    private static Path scenarioRoot;

    @BeforeAll
    static void findScenarioRoot() throws URISyntaxException {
        var url = WorkflowLogTest.class.getClassLoader().getResource("scenarios/as-du-ciel");
        assertNotNull(url, "Test fixture scenarios/as-du-ciel not found in test resources");
        scenarioRoot = Path.of(url.toURI());
    }

    // ------------------------------------------------------------------
    // Plan phase
    // ------------------------------------------------------------------

    @Test
    void planWorkflow_standard_logsAllPlanAgents() {
        CapturingLogPort log = new CapturingLogPort();
        var mock = MockModelCallPort.builder(8192)
            .when("editeur narratif",                      MockModelCallPort.CRITIC_PASS)
            .when("verificateur de coherence",             MockModelCallPort.CRITIC_PASS)
            .when("PLAN DE CHAPITRE remplit son objectif", MockModelCallPort.SCORE_PASS)
            .when("archiviste d'un roman",                 "Resume du chapitre.")
            .otherwise(MockModelCallPort.UNIVERSAL_PASS)
            .build();

        RedacteurModule.assemble(mock, new ScenarioFileAdapter(), log)
            .generate(scenarioRoot, GenerationConfig.simple());

        assertTrue(log.hasPhase("PLAN"),                 "Expected PLAN phase header");
        assertTrue(log.hasStep("ChapterPlanner"),        "Expected ChapterPlanner step");
        assertTrue(log.hasCritic("PlanNarrativeCritic"), "Expected PlanNarrativeCritic logged");
        assertTrue(log.hasCritic("PlanCoherenceCritic"), "Expected PlanCoherenceCritic logged");
        assertTrue(log.hasCritic("PlanGoalChecker"),     "Expected PlanGoalChecker logged");
        assertTrue(log.hasScore("PASS"),                 "Expected PASS outcome logged");
    }

    // ------------------------------------------------------------------
    // Write phase
    // ------------------------------------------------------------------

    @Test
    void writeWorkflow_draft_logsAllWriterSteps() {
        CapturingLogPort log = new CapturingLogPort();
        var mock = MockModelCallPort.builder(8192)
            .when("filtres les ressources narratives", "FOCUS:\nACTIONS:")
            .when("ecrivain",
                  "Il faisait beau sur la Manche ce matin-la. Pierre ajusta ses lunettes et regarda le ciel.")
            .when("fautes de langue",          "PAS D'ERREUR")
            .when("tracker d'etat narratif",   "AUCUN")
            .when("repetitions dans un roman", "EXPRESSIONS:\nSCHEMAS:")
            .when("archiviste d'un roman",     "Resume du chapitre.")
            .otherwise(MockModelCallPort.UNIVERSAL_PASS)
            .build();

        RedacteurModule.assemble(mock, new ScenarioFileAdapter(), log)
            .generate(scenarioRoot, GenerationConfig.brouillon());

        assertTrue(log.hasPhase("WRITE"),                "Expected WRITE phase header");
        assertTrue(log.hasStep("WriterStep"),            "Expected WriterStep logged");
        assertTrue(log.hasStep("ProofreaderStep"),       "Expected ProofreaderStep logged");
        assertTrue(log.hasStep("StateExtractorStep"),    "Expected StateExtractorStep logged");
        assertTrue(log.hasStep("RepetitionTrackerStep"), "Expected RepetitionTrackerStep logged");
        assertTrue(log.hasStep("RepetitionFilterStep"),  "Expected RepetitionFilterStep logged");
    }

    // ------------------------------------------------------------------
    // Eval phase
    // ------------------------------------------------------------------

    @Test
    void evaluateWorkflow_alwaysLogsStoryCompressor() {
        CapturingLogPort log = new CapturingLogPort();
        var mock = MockModelCallPort.builder(8192)
            .when("archiviste d'un roman", "Resume du chapitre.")
            .otherwise(MockModelCallPort.UNIVERSAL_PASS)
            .build();

        RedacteurModule.assemble(mock, new ScenarioFileAdapter(), log)
            .generate(scenarioRoot, GenerationConfig.brouillon());

        assertTrue(log.hasStep("StoryCompressor"), "Expected StoryCompressor in EVAL phase");
    }

    // ------------------------------------------------------------------
    // Mode PLAN_ONLY
    // ------------------------------------------------------------------

    @Test
    void planOnly_doesNotLogWriteOrEval() {
        CapturingLogPort log = new CapturingLogPort();
        var mock = MockModelCallPort.builder(8192)
            .when("editeur narratif",                      MockModelCallPort.CRITIC_PASS)
            .when("verificateur de coherence",             MockModelCallPort.CRITIC_PASS)
            .when("PLAN DE CHAPITRE remplit son objectif", MockModelCallPort.SCORE_PASS)
            .when("archiviste d'un roman",                 "Resume.")
            .otherwise(MockModelCallPort.UNIVERSAL_PASS)
            .build();

        RedacteurModule.assemble(mock, new ScenarioFileAdapter(), log)
            .generate(scenarioRoot, GenerationConfig.planOnly());

        assertTrue(log.hasPhase("PLAN"),              "Expected PLAN logged in PLAN_ONLY mode");
        assertTrue(log.hasPhase("EVAL"),              "Expected EVAL (StoryCompressor always runs)");
        assertFalse(log.hasPhase("WRITE"),            "Expected no WRITE in PLAN_ONLY mode");
        assertFalse(log.steps.contains("WriterStep"), "Expected no WriterStep in PLAN_ONLY mode");
    }
}
