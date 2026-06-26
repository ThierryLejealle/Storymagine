package storymagine.redacteur.coeur.domaine.agent.writer.planfidelitycritic;

import java.util.List;

/** Input for PlanFidelityCritic — the beats planned and the written text to verify against them. */
public record PlanFidelityCriticInput(List<String> beats, String sequenceText) {}
