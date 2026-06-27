package storymagine.redacteur.coeur.domaine.agent.sequence.planfidelitycritic;

import java.util.List;

/** Output of PlanFidelityCritic — beats not developed in the written text. */
public record PlanFidelityCriticOutput(List<String> failures, int score) {}
