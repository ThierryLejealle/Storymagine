package storymagine.redacteur.coeur.domaine.agent.chapter.textdreamcritic;

/** Input for TextDreamCritic — evaluates oneiric quality of a dream chapter. */
public record TextDreamCriticInput(String text, String bookGoal, String realismLevel) {}
