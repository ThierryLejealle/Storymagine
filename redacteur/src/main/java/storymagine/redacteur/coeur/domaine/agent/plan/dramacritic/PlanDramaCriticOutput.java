package storymagine.redacteur.coeur.domaine.agent.plan.dramacritic;

import java.util.List;

/** Output of PlanDramaCritic — tiered findings + derived score. */
public record PlanDramaCriticOutput(List<String> problems, double score) {}
