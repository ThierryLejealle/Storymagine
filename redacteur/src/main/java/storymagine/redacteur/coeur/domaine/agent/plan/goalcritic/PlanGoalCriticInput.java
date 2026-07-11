package storymagine.redacteur.coeur.domaine.agent.plan.goalcritic;

/**
 * Input for PlanGoalCritic (axis A: fidelity to the author's instruction).
 *
 * @param sequenceDirectives numbered per-sequence directives (raw author text)
 * @param language           scenario target language (ISO code, e.g. "fr")
 */
public record PlanGoalCriticInput(
    String plan,
    String chapterDescription,
    String chapterGoal,
    String sequenceDirectives,
    String bookGoal,
    String language
) {}
