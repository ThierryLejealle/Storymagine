package storymagine.redacteur.coeur.domaine.agent.plan.narrativecritic;

import java.util.List;

/** Output of PlanNarrativeCritic — tiered findings + derived score. */
public record PlanNarrativeCriticOutput(List<String> problems, double score) {}
