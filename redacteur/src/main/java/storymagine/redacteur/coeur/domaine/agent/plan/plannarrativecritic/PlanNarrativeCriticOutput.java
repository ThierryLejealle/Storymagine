package storymagine.redacteur.coeur.domaine.agent.plan.plannarrativecritic;

import java.util.List;

/** Output of PlanNarrativeCritic — tiered findings + derived score. */
public record PlanNarrativeCriticOutput(List<String> problems, double score) {}
