package storymagine.redacteur.coeur.domaine.agent.plan.coherencecritic;

/** Input for PlanCoherenceCritic. Per-sequence elements (checks, constraints, focus, lore) are embedded in the plan JSON. */
public record PlanCoherenceCriticInput(
    String plan,
    String chapterGoal,
    String checks,
    String constraints,
    String characters,
    String focusText
) {}
