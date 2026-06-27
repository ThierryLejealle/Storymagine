package storymagine.redacteur.coeur.domaine.agent.sequence.checkcritic;

import java.util.List;

/** Input for CheckCritic — the checks to verify and the written text. */
public record CheckCriticInput(List<String> checks, String sequenceText) {}
