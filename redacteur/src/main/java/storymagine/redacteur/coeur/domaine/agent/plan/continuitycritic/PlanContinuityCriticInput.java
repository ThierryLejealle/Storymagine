package storymagine.redacteur.coeur.domaine.agent.plan.continuitycritic;

/**
 * Input for PlanContinuityCritic (axis D: continuity with the story already written).
 *
 * @param chapterDescription used only for the primacy rule (a break the instruction asks for
 *                            is never a defect)
 * @param summary            running story summary (empty for the book's first chapter)
 * @param language           scenario target language (ISO code, e.g. "fr")
 */
public record PlanContinuityCriticInput(
    String plan,
    String chapterDescription,
    String chapterGoal,
    String summary,
    String language
) {}
