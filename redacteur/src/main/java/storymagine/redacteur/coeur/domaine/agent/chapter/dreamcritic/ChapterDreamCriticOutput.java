package storymagine.redacteur.coeur.domaine.agent.chapter.dreamcritic;

import java.util.List;

/** Output of ChapterDreamCritic — problems + score (coherenceScore not applicable for dreams). */
public record ChapterDreamCriticOutput(List<String> problems, double score) {}
