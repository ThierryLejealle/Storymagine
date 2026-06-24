package storymagine.redacteur.coeur.domaine.agent.plan.plancoherencecritic;

import java.util.List;

/** Output of PlanCoherenceCritic — tiered findings + derived score. */
public record PlanCoherenceCriticOutput(List<String> problems, double score) {}
