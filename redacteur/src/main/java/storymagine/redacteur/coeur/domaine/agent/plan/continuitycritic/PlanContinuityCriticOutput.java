package storymagine.redacteur.coeur.domaine.agent.plan.continuitycritic;

import java.util.List;

/** Output of PlanContinuityCritic — tiered findings + derived score. */
public record PlanContinuityCriticOutput(List<String> problems, double score) {}
