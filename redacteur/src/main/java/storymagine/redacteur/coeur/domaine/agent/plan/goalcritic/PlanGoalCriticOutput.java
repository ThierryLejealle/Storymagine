package storymagine.redacteur.coeur.domaine.agent.plan.goalcritic;

import java.util.List;

/** Output of PlanGoalCritic — tiered findings + derived score. */
public record PlanGoalCriticOutput(List<String> problems, double score) {}
