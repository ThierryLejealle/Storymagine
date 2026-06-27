package storymagine.redacteur.coeur.domaine.agent.chapter.coherencecritic;

import java.util.List;

/** Output of TextCoherenceCritic — tiered findings + derived score. */
public record TextCoherenceCriticOutput(List<String> problems, double score) {}
