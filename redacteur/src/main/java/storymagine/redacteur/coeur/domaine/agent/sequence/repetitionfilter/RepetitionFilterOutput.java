package storymagine.redacteur.coeur.domaine.agent.sequence.repetitionfilter;

import java.util.List;

/** Output of RepetitionFilter — candidates minus those that overlap a protected leitmotiv. */
public record RepetitionFilterOutput(List<String> filteredCandidates) {}
