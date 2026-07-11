package storymagine.redacteur.coeur.domaine.agent.plan.goalcritic;

import java.util.List;
import java.util.stream.Collectors;

/** Output of PlanGoalCritic. */
public record PlanGoalCriticOutput(List<String> problems, double score) {
    public String problemsText() {
        return problems.isEmpty() ? "" : problems.stream().map(p -> "- " + p).collect(Collectors.joining("\n"));
    }
}
