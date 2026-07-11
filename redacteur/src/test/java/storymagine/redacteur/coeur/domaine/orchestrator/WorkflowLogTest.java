package storymagine.redacteur.coeur.domaine.orchestrator;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import storymagine.redacteur.RedacteurModule;
import storymagine.redacteur.infra.scenario.ScenarioFileAdapter;

import java.net.URISyntaxException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the log integration of ChapterPlanWorkflow, WriteWorkflow, and EvaluateWorkflow
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
        assertTrue(log.hasCritic("PlanGoalCritic"),      "Expected PlanGoalCritic logged");
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
            .when("tracker d'etat narratif",   "AUCUN")
            .when("repetitions dans un roman", "EXPRESSIONS:\nSCHEMAS:")
            .when("archiviste d'un roman",     "Resume du chapitre.")
            .otherwise(MockModelCallPort.UNIVERSAL_PASS)
            .build();

        RedacteurModule.assemble(mock, new ScenarioFileAdapter(), log)
            .generate(scenarioRoot, GenerationConfig.brouillon());

        assertTrue(log.hasPhase("WRITE"),                         "Expected WRITE phase header");
        assertTrue(log.hasStep("SequenceWriter"),                 "Expected SequenceWriter logged");
        assertTrue(log.hasStep("SequenceStateExtractor"),         "Expected SequenceStateExtractor logged");
        assertTrue(log.hasStep("SequenceRepetitionTracker"),      "Expected SequenceRepetitionTracker logged");
        assertTrue(log.hasStep("SequenceRepetitionFilter"),       "Expected SequenceRepetitionFilter logged");
        assertFalse(log.hasStep("SequenceGrammarCorrector"),      "BROUILLON skips correctors (minimum agents only)");
    }

    // ------------------------------------------------------------------
    // Eval phase
    // ------------------------------------------------------------------

    @Test
    void evaluateWorkflow_alwaysLogsChapterSummarizer() {
        CapturingLogPort log = new CapturingLogPort();
        var mock = MockModelCallPort.builder(8192)
            .when("archiviste d'un roman", "Resume du chapitre.")
            .otherwise(MockModelCallPort.UNIVERSAL_PASS)
            .build();

        RedacteurModule.assemble(mock, new ScenarioFileAdapter(), log)
            .generate(scenarioRoot, GenerationConfig.brouillon());

        assertTrue(log.hasStep("ChapterSummarizer"), "Expected ChapterSummarizer in EVAL phase");
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

        assertTrue(log.hasPhase("PLAN"),                    "Expected PLAN logged in PLAN_ONLY mode");
        assertTrue(log.hasPhase("POST-PROD"),               "Expected POST-PROD (ChapterSummarizer always runs)");
        assertFalse(log.hasPhase("WRITE"),                  "Expected no WRITE in PLAN_ONLY mode");
        assertFalse(log.steps.contains("SequenceWriter"),   "Expected no SequenceWriter in PLAN_ONLY mode");
    }
}
