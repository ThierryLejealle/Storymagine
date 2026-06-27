package storymagine.redacteur.coeur.domaine.agent.chapter.goaltextcritic;

import java.util.List;
import java.util.stream.Collectors;

/** Output of GoalTextCritic. */
public record GoalTextCriticOutput(List<String> problems, double score) {
    public String problemsText() {
        return problems.isEmpty() ? "" : problems.stream().map(p -> "- " + p).collect(Collectors.joining("\n"));
    }
}
