package storymagine.redacteur.coeur.domaine.agent.writer.sequencechecker;

import java.util.List;

/** Output of SequenceChecker — missing or under-developed elements + score. */
public record SequenceCheckerOutput(List<String> failures, int score) {}
