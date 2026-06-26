package storymagine.redacteur.coeur.domaine.agent.writer.naturalitycorrector;

/** A single naturalness finding: the artificial phrase to replace and its corrected rewrite. */
public record NaturalityFinding(String wrongPhrase, String correctedPhrase) {}
