package storymagine.redacteur.coeur.domaine.agent.temp.causalanalyzer;

import java.util.List;

/** Output of CausalAnalyzer â€” causal coherence findings + score. */
public record CausalAnalyzerOutput(List<String> findings, int score) {}
