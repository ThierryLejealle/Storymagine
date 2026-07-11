package storymagine.redacteur.coeur.domaine.agent.chapter.whatifcritic;

import java.util.List;

/** Output of ChapterWhatIfCritic — plausibility problems + score. */
public record ChapterWhatIfCriticOutput(List<String> problems, double score) {}
