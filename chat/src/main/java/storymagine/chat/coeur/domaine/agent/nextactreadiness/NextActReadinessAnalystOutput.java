package storymagine.chat.coeur.domaine.agent.nextactreadiness;

/**
 * conditionUnderstood restates the [NEXT ACT] condition in the model's own words ; currentState
 * says how much of it has actually happened in the story so far ; missing is what still needs to
 * happen ("[RIEN]" from the model becomes the empty string here — see NextActReadinessAnalyst).
 */
public record NextActReadinessAnalystOutput(String conditionUnderstood, String currentState, String missing) {
}
