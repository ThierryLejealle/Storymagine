package storymagine.chat.coeur.domaine.agent.continuityreviewer;

import storymagine.chat.coeur.domaine.scenario.ActNumber;

public record ScenarioContinuityReviewerInput(String characterSheet, String premise, String storySoFar,
                                               ActNumber actNumber, String actTitle, String actText) {
}
