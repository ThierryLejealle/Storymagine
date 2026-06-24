package storymagine.redacteur.coeur.domaine.agent.writer.sequencestylechecker;

import java.util.List;

/** Output of SequenceStyleChecker. */
public record SequenceStyleCheckerOutput(List<String> problems, int score) {
    public boolean passed(int threshold) { return score >= threshold; }
}
