package storymagine.redacteur.coeur.domaine.agent.writer.stylecorrector;

/** A single style correction: exact phrase to replace and its corrected form. */
public record StyleCorrectorFinding(String wrongPhrase, String correctedPhrase) {}
