package storymagine.redacteur.coeur.domaine.agent.plan.factscritic;

/**
 * Input for PlanFactsCritic (axis B: respect of already-established facts).
 *
 * @param chapterDescription used only for the primacy rule (a change the instruction asks for
 *                            is never a defect)
 * @param language           scenario target language (ISO code, e.g. "fr")
 */
public record PlanFactsCriticInput(
    String plan,
    String chapterDescription,
    String chapterGoal,
    String characters,
    String checks,
    String entityState,
    String language
) {}
