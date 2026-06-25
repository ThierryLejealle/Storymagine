package storymagine.redacteur.coeur.domaine.agent.writer.naturalityfilter;

import java.util.List;

/** Output of NaturalityFilter — list of artificial phrases with suggested rewrites. */
public record NaturalityFilterOutput(List<NaturalityFinding> findings) {}
