package storymagine.redacteur.coeur.domaine.agent.chapter.dreamcritic;

import java.util.List;

/** Output of TextDreamCritic — problems + score (coherenceScore not applicable for dreams). */
public record TextDreamCriticOutput(List<String> problems, double score) {}
