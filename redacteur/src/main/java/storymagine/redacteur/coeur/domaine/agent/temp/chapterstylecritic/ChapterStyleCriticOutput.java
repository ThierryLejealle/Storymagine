package storymagine.redacteur.coeur.domaine.agent.temp.chapterstylecritic;

import java.util.List;

/** Output of ChapterStyleCritic â€” style problems + score. */
public record ChapterStyleCriticOutput(List<String> problems, int score) {
    public boolean passed(int threshold) { return score >= threshold; }
}
