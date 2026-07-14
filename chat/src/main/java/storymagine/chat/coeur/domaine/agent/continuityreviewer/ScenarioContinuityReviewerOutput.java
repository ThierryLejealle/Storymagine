package storymagine.chat.coeur.domaine.agent.continuityreviewer;

import java.util.List;

public record ScenarioContinuityReviewerOutput(List<String> issues, List<String> suggestions) {
}
