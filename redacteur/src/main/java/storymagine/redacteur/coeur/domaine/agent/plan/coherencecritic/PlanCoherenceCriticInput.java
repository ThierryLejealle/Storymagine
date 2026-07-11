package storymagine.redacteur.coeur.domaine.agent.plan.coherencecritic;

/** Input for PlanCoherenceCritic. Per-sequence points à vérifier (focus, lore) are embedded in the plan JSON. */
public record PlanCoherenceCriticInput(
    String plan,
    String description,
    String chapterGoal,
    String checks,
    String characters,
    String entityState
) {}
