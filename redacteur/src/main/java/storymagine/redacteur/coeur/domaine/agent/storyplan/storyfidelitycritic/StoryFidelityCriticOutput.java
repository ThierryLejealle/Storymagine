package storymagine.redacteur.coeur.domaine.agent.storyplan.storyfidelitycritic;

import java.util.List;

/** Output of StoryFidelityCritic — tiered findings + derived score. */
public record StoryFidelityCriticOutput(List<String> problems, double score) {}
