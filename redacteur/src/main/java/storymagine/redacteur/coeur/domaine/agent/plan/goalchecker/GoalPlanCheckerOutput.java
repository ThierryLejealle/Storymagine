package storymagine.redacteur.coeur.domaine.agent.plan.goalchecker;

import java.util.List;
import java.util.stream.Collectors;

/** Output of GoalPlanChecker. */
public record GoalPlanCheckerOutput(List<String> problems, double score) {
    public String problemsText() {
        return problems.isEmpty() ? "" : problems.stream().map(p -> "- " + p).collect(Collectors.joining("\n"));
    }
}
