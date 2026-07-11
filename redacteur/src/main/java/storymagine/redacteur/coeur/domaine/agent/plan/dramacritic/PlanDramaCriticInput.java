package storymagine.redacteur.coeur.domaine.agent.plan.dramacritic;

/**
 * Input for PlanDramaCritic (axis E: dramaturgical effort, relative to the intensity the
 * instruction itself calls for — never an absolute tension level).
 *
 * @param sequenceDirectives per-sequence directives — may set a calmer or more intense moment
 *                           than the chapter as a whole, and always prevails at that granularity
 * @param language           scenario target language (ISO code, e.g. "fr")
 */
public record PlanDramaCriticInput(
    String plan,
    String chapterDescription,
    String chapterGoal,
    String sequenceDirectives,
    String language
) {}
