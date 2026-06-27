package storymagine.redacteur.coeur.domaine.agent.chapter.narrativecritic;

import java.util.List;

/** Output of TextNarrativeCritic — tiered findings + derived score. */
public record TextNarrativeCriticOutput(List<String> problems, double score) {}
