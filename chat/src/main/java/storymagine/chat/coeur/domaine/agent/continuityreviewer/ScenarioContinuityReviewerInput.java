package storymagine.chat.coeur.domaine.agent.continuityreviewer;

import storymagine.chat.coeur.domaine.scenario.ActNumber;
import storymagine.chat.coeur.domaine.scenario.ScenarioLanguage;

public record ScenarioContinuityReviewerInput(String characterSheet, String premise, String storySoFar,
                                               ActNumber actNumber, String actTitle, String actText,
                                               ScenarioLanguage language) {
}
