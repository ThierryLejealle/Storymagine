package storymagine.redacteur.coeur.domaine.agent.writer.naturalityfilter;

/** A single naturalness finding: the artificial phrase, the problem, and a rewrite suggestion. */
public record NaturalityFinding(String citation, String probleme, String suggestion) {}
