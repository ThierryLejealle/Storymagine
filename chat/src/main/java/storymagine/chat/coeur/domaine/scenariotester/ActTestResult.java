package storymagine.chat.coeur.domaine.scenariotester;

import storymagine.chat.coeur.domaine.scenario.ActNumber;

import java.util.List;

/**
 * One act's outcome from a scenario test run : the narrative-continuity issues found by
 * ScenarioContinuityReviewer, the usability issues found by ScenarioClarityReviewer, and the
 * improvement suggestions from both, merged and tagged by origin (never scored — see
 * ScenarioTesterServiceImpl).
 */
public record ActTestResult(ActNumber actNumber, String actTitle, List<String> continuityIssues,
                             List<String> clarityIssues, List<String> suggestions) {
}
