package storymagine.redacteur.coeur.domaine.agent.chapter.textdreamcritic;

import java.util.List;

/** Output of TextDreamCritic — problems + score (coherenceScore not applicable for dreams). */
public record TextDreamCriticOutput(List<String> problems, double score) {}
