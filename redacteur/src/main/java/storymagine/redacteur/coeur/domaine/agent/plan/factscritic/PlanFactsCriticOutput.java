package storymagine.redacteur.coeur.domaine.agent.plan.factscritic;

import java.util.List;

/** Output of PlanFactsCritic — tiered findings + derived score. */
public record PlanFactsCriticOutput(List<String> problems, double score) {}
