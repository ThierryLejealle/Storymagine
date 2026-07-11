package storymagine.redacteur.coeur.domaine.agent.plan.coherencecritic;

/**
 * Input for PlanCoherenceCritic (axis C: internal coherence of the plan).
 *
 * @param sequenceDirectives numbered per-sequence directives — used only for the primacy rule
 *                            (a rupture the directives explicitly ask for is never a defect)
 * @param language           scenario target language (ISO code, e.g. "fr")
 */
public record PlanCoherenceCriticInput(
    String plan,
    String sequenceDirectives,
    String language
) {}
