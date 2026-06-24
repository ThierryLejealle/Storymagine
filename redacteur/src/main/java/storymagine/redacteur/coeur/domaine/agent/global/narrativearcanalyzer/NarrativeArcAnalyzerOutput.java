package storymagine.redacteur.coeur.domaine.agent.global.narrativearcanalyzer;

import java.util.List;

/** Output of NarrativeArcAnalyzer — arc findings + score. */
public record NarrativeArcAnalyzerOutput(List<String> findings, int score) {}
