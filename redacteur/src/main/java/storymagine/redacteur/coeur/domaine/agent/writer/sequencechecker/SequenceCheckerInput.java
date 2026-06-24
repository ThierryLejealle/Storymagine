package storymagine.redacteur.coeur.domaine.agent.writer.sequencechecker;

import java.util.List;

/** Input for SequenceChecker — verifies that all required elements are present. */
public record SequenceCheckerInput(
    String sequenceDescription,
    String sequenceText,
    List<String> checks
) {}
