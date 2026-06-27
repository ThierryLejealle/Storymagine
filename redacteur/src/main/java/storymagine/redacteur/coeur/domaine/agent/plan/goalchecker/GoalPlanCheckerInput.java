package storymagine.redacteur.coeur.domaine.agent.plan.goalchecker;

/** Input for GoalPlanChecker — checks that a chapter plan fulfils its declared narrative goal. */
public record GoalPlanCheckerInput(String plan, String chapterGoal, String bookGoal) {}
