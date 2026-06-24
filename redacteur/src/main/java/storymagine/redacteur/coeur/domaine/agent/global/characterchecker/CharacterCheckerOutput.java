package storymagine.redacteur.coeur.domaine.agent.global.characterchecker;

import java.util.List;
import java.util.stream.Collectors;

/** Output of CharacterChecker — coherence issues + score. */
public record CharacterCheckerOutput(List<String> issues, int score) {
    public boolean hasIssues() { return !issues.isEmpty(); }
    public String summary() {
        return issues.isEmpty() ? "aucune incohérence détectée"
            : issues.size() + " incohérence(s) : " + issues.stream().collect(Collectors.joining(" | "));
    }
}
