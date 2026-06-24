package storymagine.redacteur.coeur.domaine.agent.global.causalanalyzer;

import java.util.List;

/** Output of CausalAnalyzer — causal coherence findings + score. */
public record CausalAnalyzerOutput(List<String> findings, int score) {}
