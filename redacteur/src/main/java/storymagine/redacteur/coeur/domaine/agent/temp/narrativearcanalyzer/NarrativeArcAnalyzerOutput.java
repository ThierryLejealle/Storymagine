package storymagine.redacteur.coeur.domaine.agent.temp.narrativearcanalyzer;

import java.util.List;

/** Output of NarrativeArcAnalyzer â€” arc findings + score. */
public record NarrativeArcAnalyzerOutput(List<String> findings, int score) {}
