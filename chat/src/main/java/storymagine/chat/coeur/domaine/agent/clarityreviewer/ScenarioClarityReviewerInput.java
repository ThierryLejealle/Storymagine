package storymagine.chat.coeur.domaine.agent.clarityreviewer;

import storymagine.chat.coeur.domaine.scenario.ActNumber;
import storymagine.chat.coeur.domaine.scenario.ScenarioLanguage;

public record ScenarioClarityReviewerInput(String characterSheet, String premise,
                                            ActNumber actNumber, String actTitle, String actText,
                                            ScenarioLanguage language) {
}
