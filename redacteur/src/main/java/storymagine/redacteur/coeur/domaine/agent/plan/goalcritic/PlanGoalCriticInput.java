package storymagine.redacteur.coeur.domaine.agent.plan.goalcritic;

/** Input for PlanGoalCritic — checks that a chapter plan fulfils its declared narrative goal. */
public record PlanGoalCriticInput(String plan, String chapterDescription, String chapterGoal, String bookGoal) {}
