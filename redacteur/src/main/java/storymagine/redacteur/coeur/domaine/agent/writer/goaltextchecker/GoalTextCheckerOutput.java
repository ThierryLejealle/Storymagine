package storymagine.redacteur.coeur.domaine.agent.writer.goaltextchecker;

import java.util.List;
import java.util.stream.Collectors;

/** Output of GoalTextChecker. */
public record GoalTextCheckerOutput(List<String> problems, double score) {
    public String problemsText() {
        return problems.isEmpty() ? "" : problems.stream().map(p -> "- " + p).collect(Collectors.joining("\n"));
    }
}
