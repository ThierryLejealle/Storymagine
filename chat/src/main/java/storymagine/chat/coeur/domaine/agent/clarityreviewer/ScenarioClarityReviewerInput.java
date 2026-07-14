package storymagine.chat.coeur.domaine.agent.clarityreviewer;

import storymagine.chat.coeur.domaine.scenario.ActNumber;

public record ScenarioClarityReviewerInput(String characterSheet, String premise,
                                            ActNumber actNumber, String actTitle, String actText) {
}
