package storymagine.redacteur.coeur.domaine.agent.chapter.goalcritic;

import java.util.List;
import java.util.stream.Collectors;

/** Output of ChapterGoalCritic. */
public record ChapterGoalCriticOutput(List<String> problems, double score) {
    public String problemsText() {
        return problems.isEmpty() ? "" : problems.stream().map(p -> "- " + p).collect(Collectors.joining("\n"));
    }
}
