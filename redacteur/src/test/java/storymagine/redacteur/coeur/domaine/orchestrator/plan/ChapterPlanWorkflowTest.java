package storymagine.redacteur.coeur.domaine.orchestrator.plan;

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
 * Tests ChapterPlanWorkflow's retry/threshold mechanics across the 5-critic pool (goal, facts,
 * coherence, continuity, drama), using scripted ("fake") critic responses via MockModelCallPort
 * — no real Ollama calls. Written after a real production bug (2026-07-11) where a prompt-format
 * mismatch made every plan critic silently score a perfect 10.0 regardless of actual findings,
 * which nothing in the test suite would have caught: there was no coverage at all of the plan
 * retry/elimination logic with anything other than a uniformly clean response.
 *
 * Fixture "as-du-ciel" has 8 chapters — SIMPLE quality level (planMaxRetry=1, so 2 attempts max
 * per chapter; planAverageThreshold=7.0; planEliminationThreshold=3.0).
 */
class ChapterPlanWorkflowTest {

    private static Path scenarioRoot;
    private static final int CHAPTER_COUNT = 8;

    /** Score ≈ 2.5 (majBase(3), well below SIMPLE's elimination threshold of 3.0). */
    private static final String THREE_MAJOR_DEFECTS = """
            AMELIORATION: [RIEN]
            DEFAUT_SIGNIFICATIF: [RIEN]
            DEFAUT_MAJEUR: probleme 1
            DEFAUT_MAJEUR: probleme 2
            DEFAUT_MAJEUR: probleme 3
            """;

    /** Score = 9.5 (amelBase(1)) — passes every level's average/elimination comfortably. */
    private static final String ONE_AMELIORATION = """
            AMELIORATION: detail mineur ameliorable
            DEFAUT_SIGNIFICATIF: [RIEN]
            DEFAUT_MAJEUR: [RIEN]
            """;

    @BeforeAll
    static void findScenarioRoot() throws URISyntaxException {
        var url = ChapterPlanWorkflowTest.class.getClassLoader().getResource("scenarios/as-du-ciel");
        assertNotNull(url, "Test fixture scenarios/as-du-ciel not found in test resources");
        scenarioRoot = Path.of(url.toURI());
    }

    /**
     * Number of ChapterPlanWorkflow attempts across all chapters — i.e. exact "PLAN" phase
     * headers, minus the one StoryOrchestrator always logs once per generate() call under the
     * same exact label. countPhase("PLAN") is not precise enough here: it also matches "PLAN
     * LIVRE" and "PLAN chapitre N/M" (one per chapter, not per attempt), both logged elsewhere.
     */
    private static long chapterPlanAttempts(CapturingLogPort log) {
        return log.phases.stream().filter(p -> p.equals("PLAN")).count() - 1;
    }

    /**
     * Number of RETRY outcomes logged anywhere in the run. Unlike PASS, this is unambiguous in
     * these tests: the WRITE phase always passes cleanly (UNIVERSAL_PASS), so every RETRY here
     * can only come from ChapterPlanWorkflow's own scoresSummary calls.
     */
    private static long retryCount(CapturingLogPort log) {
        return log.scores.stream().filter(s -> s.equals("RETRY")).count();
    }

    @Test
    void allFiveCriticsPass_singleAttemptPerChapter_noRetry() {
        CapturingLogPort log = new CapturingLogPort();
        var mock = MockModelCallPort.builder(8192)
            .when("archiviste d'un roman", "Resume du chapitre.")
            .otherwise(MockModelCallPort.UNIVERSAL_PASS)
            .build();

        RedacteurModule.assemble(mock, new ScenarioFileAdapter(), log)
            .generate(scenarioRoot, GenerationConfig.simple());

        assertTrue(log.hasCritic("PlanGoalCritic"));
        assertTrue(log.hasCritic("PlanFactsCritic"));
        assertTrue(log.hasCritic("PlanCoherenceCritic"));
        assertTrue(log.hasCritic("PlanContinuityCritic"));
        assertTrue(log.hasCritic("PlanDramaCritic"));
        assertEquals(CHAPTER_COUNT, chapterPlanAttempts(log),
            "One passing attempt per chapter — no retry when all 5 critics are clean");
        assertFalse(log.hasScore("RETRY"), "No RETRY outcome expected when every critic passes");
    }

    @Test
    void oneCriticBelowEliminationThreshold_forcesRetry_everyChapterUsesBothAttempts() {
        CapturingLogPort log = new CapturingLogPort();
        var mock = MockModelCallPort.builder(8192)
            // PlanCoherenceCritic always scores ~2.5 (below the 3.0 elimination threshold),
            // regardless of what the other 4 critics say — every attempt must retry.
            .when("You are PlanCoherenceCritic", THREE_MAJOR_DEFECTS)
            .when("archiviste d'un roman", "Resume du chapitre.")
            .otherwise(MockModelCallPort.UNIVERSAL_PASS)
            .build();

        RedacteurModule.assemble(mock, new ScenarioFileAdapter(), log)
            .generate(scenarioRoot, GenerationConfig.simple());

        // SIMPLE allows 1 retry (2 attempts). A critic stuck below the elimination threshold on
        // every attempt means every chapter exhausts both attempts before giving up.
        assertEquals(CHAPTER_COUNT * 2, chapterPlanAttempts(log),
            "Every chapter must retry once (2 attempts) when one critic never clears "
            + "the elimination threshold");
        assertEquals(CHAPTER_COUNT * 2, retryCount(log),
            "Every attempt of every chapter must report RETRY while PlanCoherenceCritic stays eliminated");
    }

    @Test
    void eliminationThreshold_overridesAPassingAverage() {
        CapturingLogPort log = new CapturingLogPort();
        var mock = MockModelCallPort.builder(8192)
            // Average with this critic at ~2.5 and the other 4 at 10 is (10*4+2.5)/5 = 8.5,
            // comfortably above SIMPLE's planAverageThreshold (7.0) — only the elimination rule
            // (any individual critic below 3.0) can explain a forced retry here.
            .when("You are PlanDramaCritic", THREE_MAJOR_DEFECTS)
            .when("archiviste d'un roman", "Resume du chapitre.")
            .otherwise(MockModelCallPort.UNIVERSAL_PASS)
            .build();

        RedacteurModule.assemble(mock, new ScenarioFileAdapter(), log)
            .generate(scenarioRoot, GenerationConfig.simple());

        assertEquals(CHAPTER_COUNT * 2, chapterPlanAttempts(log),
            "A single critic below the elimination threshold must force a retry even though "
            + "the 5-critic average clears planAverageThreshold");
        assertEquals(CHAPTER_COUNT * 2, retryCount(log),
            "Elimination must force a RETRY on every attempt of every chapter despite the passing average");
    }

    @Test
    void full_firstDraftHasARemark_forcesExactlyOneRetry_evenWhenAverageAndEliminationPass() {
        CapturingLogPort log = new CapturingLogPort();
        var mock = MockModelCallPort.builder(8192)
            // A single AMELIORATION scores 9.5 — comfortably above FULL's planAverageThreshold
            // (8.0) and planEliminationThreshold (5.5). Only the FULL-only "strict first
            // attempt" rule can explain a forced retry here.
            .when("You are PlanCoherenceCritic", ONE_AMELIORATION)
            .when("archiviste d'un roman", "Resume du chapitre.")
            .otherwise(MockModelCallPort.UNIVERSAL_PASS)
            .build();

        RedacteurModule.assemble(mock, new ScenarioFileAdapter(), log)
            .generate(scenarioRoot, GenerationConfig.full());

        // The mock returns the same single-AMELIORATION response on every attempt, so the
        // second attempt scores identically to the first — the strict rule only fires on
        // attempt 1, so the second attempt is accepted despite carrying the same remark.
        assertEquals(CHAPTER_COUNT * 2, chapterPlanAttempts(log),
            "FULL: any remark at all on the first draft must force exactly one retry, even "
            + "though average and elimination both pass comfortably");
        assertTrue(log.warnings.stream().anyMatch(w -> w.contains("premier jet")),
            "The forced retry must be explained in the logs");
        assertEquals(CHAPTER_COUNT, log.planRetentions.size(),
            "Every chapter used more than one attempt, so each must log which attempt/score was retained");
        assertTrue(log.planRetentions.stream().allMatch(r -> r.startsWith("1/2@")),
            "The retained attempt must be reported with its number and score: " + log.planRetentions);
    }

    @Test
    void simple_firstDraftHasARemark_doesNotForceRetry_strictRuleIsFullOnly() {
        CapturingLogPort log = new CapturingLogPort();
        var mock = MockModelCallPort.builder(8192)
            .when("You are PlanCoherenceCritic", ONE_AMELIORATION)
            .when("archiviste d'un roman", "Resume du chapitre.")
            .otherwise(MockModelCallPort.UNIVERSAL_PASS)
            .build();

        RedacteurModule.assemble(mock, new ScenarioFileAdapter(), log)
            .generate(scenarioRoot, GenerationConfig.simple());

        assertEquals(CHAPTER_COUNT, chapterPlanAttempts(log),
            "SIMPLE has no strict-first-attempt rule — a single AMELIORATION must not force a "
            + "retry when average and elimination both pass");
    }
}
