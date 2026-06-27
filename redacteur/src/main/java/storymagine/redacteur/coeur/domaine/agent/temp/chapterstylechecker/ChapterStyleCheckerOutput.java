package storymagine.redacteur.coeur.domaine.agent.temp.chapterstylechecker;

import java.util.List;

/** Output of ChapterStyleChecker â€” style problems + score. */
public record ChapterStyleCheckerOutput(List<String> problems, int score) {
    public boolean passed(int threshold) { return score >= threshold; }
}
