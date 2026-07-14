package storymagine.chat.coeur.domaine.scenariotester;

import java.util.List;

/** The full result of testing a ChatScenario, act by act — see ScenarioTesterServiceImpl. */
public record ScenarioTestReport(String scenarioName, List<ActTestResult> actResults) {
}
