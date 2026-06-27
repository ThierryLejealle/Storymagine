package storymagine.redacteur.coeur.domaine.agent.sequence.naturalitycorrector;

import java.util.List;

/** Output of NaturalityCorrector — list of artificial phrases with suggested rewrites. */
public record NaturalityCorrectorOutput(List<NaturalityFinding> findings) {}
