package storymagine.redacteur.coeur.domaine.agent.chapter.coherencecritic;

import java.util.List;

/** Output of ChapterCoherenceCritic — tiered findings + derived score. */
public record ChapterCoherenceCriticOutput(List<String> problems, double score) {}
