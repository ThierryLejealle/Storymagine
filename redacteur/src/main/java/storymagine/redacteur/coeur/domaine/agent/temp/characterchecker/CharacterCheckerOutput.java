package storymagine.redacteur.coeur.domaine.agent.temp.characterchecker;

import java.util.List;
import java.util.stream.Collectors;

/** Output of CharacterChecker â€” coherence issues + score. */
public record CharacterCheckerOutput(List<String> issues, int score) {
    public boolean hasIssues() { return !issues.isEmpty(); }
    public String summary() {
        return issues.isEmpty() ? "aucune incohÃ©rence dÃ©tectÃ©e"
            : issues.size() + " incohÃ©rence(s) : " + issues.stream().collect(Collectors.joining(" | "));
    }
}
