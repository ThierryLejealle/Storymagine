package storymagine.redacteur.coeur.domaine.agent.chapter.narrativecritic;

import java.util.List;

/** Output of ChapterNarrativeCritic — tiered findings + derived score. */
public record ChapterNarrativeCriticOutput(List<String> problems, double score) {}
