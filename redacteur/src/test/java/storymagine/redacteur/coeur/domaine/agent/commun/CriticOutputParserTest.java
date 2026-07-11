package storymagine.redacteur.coeur.domaine.agent.commun;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Direct unit tests for CriticOutputParser — the score/problem parser shared by ~15 Critic
 * agents. Written after a real production bug (2026-07-11): several plan critics used a
 * "header line + bullet list below it" output format that the parser silently mistook for
 * [RIEN], inflating every score to 10.0 regardless of actual findings. Never leave this parser
 * untested again.
 */
class CriticOutputParserTest {

    // ── All-clean responses ─────────────────────────────────────────────────

    @Test
    void allClean_repeatedTagFormat_score10NoProblems() {
        String response = """
                AMELIORATION: [RIEN]
                DEFAUT_SIGNIFICATIF: [RIEN]
                DEFAUT_MAJEUR: [RIEN]
                """;
        assertEquals(10.0, CriticOutputParser.calculateScore(response));
        assertTrue(CriticOutputParser.parseProblems(response).isEmpty());
    }

    @Test
    void allClean_headerBulletFormat_score10NoProblems() {
        String response = """
                AMELIORATION:
                [RIEN]
                DEFAUT_SIGNIFICATIF:
                [RIEN]
                DEFAUT_MAJEUR:
                [RIEN]
                """;
        assertEquals(10.0, CriticOutputParser.calculateScore(response));
        assertTrue(CriticOutputParser.parseProblems(response).isEmpty());
    }

    // ── Repeated-tag convention (historical — most existing critics) ───────

    @Test
    void repeatedTagFormat_multipleProblemsSameTier_allCounted() {
        String response = """
                AMELIORATION: [RIEN]
                DEFAUT_SIGNIFICATIF: le heros n'a pas d'epee
                DEFAUT_SIGNIFICATIF: l'ours est en peluche
                DEFAUT_MAJEUR: [RIEN]
                """;
        List<String> problems = CriticOutputParser.parseProblems(response);
        assertEquals(2, problems.size());
        assertTrue(problems.get(0).contains("epee"));
        assertTrue(problems.get(1).contains("peluche"));
        // sigBase(2) = 7.0 - 2*0.5 = 6.0, no amelioration correction
        assertEquals(6.0, CriticOutputParser.calculateScore(response), 0.001);
    }

    // ── Header + bullet-list convention (2026-07-11 plan critics) ──────────

    @Test
    void headerBulletFormat_singleProblem_countedNotDropped() {
        String response = """
                AMELIORATION:
                - Sequence 2: a real finding here
                DEFAUT_SIGNIFICATIF:
                [RIEN]
                DEFAUT_MAJEUR:
                [RIEN]
                """;
        List<String> problems = CriticOutputParser.parseProblems(response);
        assertEquals(1, problems.size());
        assertTrue(problems.get(0).contains("Sequence 2"));
        // amelBase(1) = 10.0 - 1*0.5 = 9.5 — must NOT be 10.0 (the bug silently dropped this).
        assertEquals(9.5, CriticOutputParser.calculateScore(response), 0.001);
    }

    @Test
    void headerBulletFormat_multipleProblemsSameTier_allCounted() {
        String response = """
                AMELIORATION: [RIEN]
                DEFAUT_SIGNIFICATIF:
                - Sequence 1: first issue
                - Sequence 3: second issue
                DEFAUT_MAJEUR: [RIEN]
                """;
        List<String> problems = CriticOutputParser.parseProblems(response);
        assertEquals(2, problems.size());
        // sigBase(2) = 6.0
        assertEquals(6.0, CriticOutputParser.calculateScore(response), 0.001);
    }

    /**
     * Regression test built from the exact response that caused the real production bug
     * (PlanFactsCritic, scenario 1998, 2026-07-11 19h26 run) — a bare "AMELIORATION:" header
     * line followed by the finding on the next line. Before the fix, this silently scored 10.0
     * with zero problems detected.
     */
    @Test
    void regressionRealPlanFactsCriticBug_findingIsNoLongerDropped() {
        String response = """
                AMELIORATION:
                - Sequence 2: Thierry arrive seul au stand d'inscription alors qu'il est arrivé avec sa sœur Catherine dans la séquence 1 et qu'ils sont en voyage ensemble.

                DEFAUT_SIGNIFICATIF:
                [RIEN]

                DEFAUT_MAJEUR:
                [RIEN]
                """;
        List<String> problems = CriticOutputParser.parseProblems(response);
        assertEquals(1, problems.size(), "The real finding must be captured, not silently dropped");
        assertNotEquals(10.0, CriticOutputParser.calculateScore(response),
                "A real AMELIORATION finding must never score a perfect 10.0");
        assertEquals(9.5, CriticOutputParser.calculateScore(response), 0.001);
    }

    // ── Trailing content after the last tier must never leak in ────────────

    @Test
    void trailingScoreLine_afterHeaderBulletFormat_doesNotInflateTierCounts() {
        // Mirrors MockModelCallPort.UNIVERSAL_PASS: "OK" prefix + trailing "SCORE: 10" suffix,
        // wrapped around an all-clean header/bullet-style response.
        String response = """
                OK
                AMELIORATION:
                [RIEN]
                DEFAUT_SIGNIFICATIF:
                [RIEN]
                DEFAUT_MAJEUR:
                [RIEN]
                SCORE: 10
                """;
        assertEquals(10.0, CriticOutputParser.calculateScore(response),
                "A trailing SCORE: line must never be miscounted as a finding of the last tier");
        assertTrue(CriticOutputParser.parseProblems(response).isEmpty());
    }

    @Test
    void trailingScoreLine_afterRepeatedTagFormat_doesNotInflateTierCounts() {
        String response = """
                OK
                AMELIORATION: [RIEN]
                DEFAUT_SIGNIFICATIF: [RIEN]
                DEFAUT_MAJEUR: [RIEN]
                SCORE: 10
                """;
        assertEquals(10.0, CriticOutputParser.calculateScore(response));
        assertTrue(CriticOutputParser.parseProblems(response).isEmpty());
    }

    // ── Sentinel variants ────────────────────────────────────────────────────

    @Test
    void sentinelVariants_allRecognizedAsEmpty() {
        for (String sentinel : List.of("[RIEN]", "AUCUN", "AUCUNE", "NONE", "NEANT",
                "aucun probleme", "rien a signaler")) {
            String response = "AMELIORATION: " + sentinel + "\n"
                    + "DEFAUT_SIGNIFICATIF: [RIEN]\n"
                    + "DEFAUT_MAJEUR: [RIEN]\n";
            assertEquals(10.0, CriticOutputParser.calculateScore(response),
                    "Sentinel '" + sentinel + "' should not count as a finding");
        }
    }

    @Test
    void sentinelVariant_asHeaderBulletContinuation_alsoRecognized() {
        String response = """
                AMELIORATION:
                AUCUNE
                DEFAUT_SIGNIFICATIF: [RIEN]
                DEFAUT_MAJEUR: [RIEN]
                """;
        assertEquals(10.0, CriticOutputParser.calculateScore(response));
        assertTrue(CriticOutputParser.parseProblems(response).isEmpty());
    }

    // ── Score formula sanity checks ─────────────────────────────────────────

    @Test
    void majeurPresent_dominatesScore() {
        String response = """
                AMELIORATION: [RIEN]
                DEFAUT_SIGNIFICATIF: [RIEN]
                DEFAUT_MAJEUR: something is badly wrong
                """;
        // majBase(1) = 4.0 - 1*0.5 = 3.5, no corrections
        assertEquals(3.5, CriticOutputParser.calculateScore(response), 0.001);
    }

    @Test
    void significatifOnly_noMajeur() {
        String response = """
                AMELIORATION: [RIEN]
                DEFAUT_SIGNIFICATIF: one real issue
                DEFAUT_MAJEUR: [RIEN]
                """;
        // sigBase(1) = 7.0 - 1*0.5 = 6.5
        assertEquals(6.5, CriticOutputParser.calculateScore(response), 0.001);
    }

    // ── Edge cases ───────────────────────────────────────────────────────────

    @Test
    void nullResponse_returnsDefaultScoreAndEmptyProblems() {
        assertEquals(5.0, CriticOutputParser.calculateScore(null));
        assertTrue(CriticOutputParser.parseProblems(null).isEmpty());
    }
}
