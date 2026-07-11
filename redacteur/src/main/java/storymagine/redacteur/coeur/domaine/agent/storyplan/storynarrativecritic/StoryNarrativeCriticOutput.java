package storymagine.redacteur.coeur.domaine.agent.storyplan.storynarrativecritic;

import java.util.List;

/** Output of StoryNarrativeCritic — tiered findings + derived score. */
public record StoryNarrativeCriticOutput(List<String> problems, double score) {}
