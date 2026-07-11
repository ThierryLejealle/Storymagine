package storymagine.redacteur.coeur.domaine.agent.storyplan.storycausalcritic;

import java.util.List;

/** Output of StoryCausalCritic — tiered findings + derived score. */
public record StoryCausalCriticOutput(List<String> problems, double score) {}
