package storymagine.chat.coeur.domaine.agent.clarityreviewer;

import java.util.List;

public record ScenarioClarityReviewerOutput(List<String> issues, List<String> suggestions) {
}
