package storymagine.redacteur.coeur.domaine.agent.writer.checkcritic;

import java.util.List;

/** Output of CheckCritic — checks that failed verification. */
public record CheckCriticOutput(List<String> failures, int score) {}
