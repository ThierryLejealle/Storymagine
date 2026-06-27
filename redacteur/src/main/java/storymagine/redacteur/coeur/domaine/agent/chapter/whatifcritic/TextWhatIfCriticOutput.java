package storymagine.redacteur.coeur.domaine.agent.chapter.whatifcritic;

import java.util.List;

/** Output of TextWhatIfCritic — plausibility problems + score. */
public record TextWhatIfCriticOutput(List<String> problems, double score) {}
