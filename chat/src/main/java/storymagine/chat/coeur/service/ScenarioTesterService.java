package storymagine.chat.coeur.service;

import storymagine.chat.coeur.domaine.scenario.ChatScenario;
import storymagine.chat.coeur.domaine.scenariotester.ScenarioTestReport;

/** Static QA of a ChatScenario : dry-runs its acts through two reviewer agents, no real chat session. */
public interface ScenarioTesterService {

    ScenarioTestReport testScenario(ChatScenario scenario);
}
