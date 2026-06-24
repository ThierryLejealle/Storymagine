package storymagine.redacteur.coeur.domaine.agent.writer.stateextractor;

/** Input for StateExtractor — sequence just written + accumulated entity state for context. */
public record StateExtractorInput(String sequenceText, String previousState) {}
