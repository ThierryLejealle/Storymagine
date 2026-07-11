package storymagine.redacteur.coeur.domaine.agent.plan.continuitycritic;

/**
 * Input for PlanContinuityCritic (axis D: continuity with the earlier chapters of this book).
 *
 * @param chapterDescription    used only for the primacy rule (a break the instruction asks for
 *                               is never a defect)
 * @param previousChaptersPlans plans already generated for the chapters before this one
 *                               (empty for the book's first chapter — nothing to compare against)
 * @param language              scenario target language (ISO code, e.g. "fr")
 */
public record PlanContinuityCriticInput(
    String plan,
    String chapterDescription,
    String chapterGoal,
    String previousChaptersPlans,
    String language
) {}
