package storymagine.redacteur.coeur.domaine.agent.writer.repetitionfilter;

import java.util.List;

/** Output of RepetitionFilter — candidates minus those that overlap a protected leitmotiv. */
public record RepetitionFilterOutput(List<String> filteredCandidates) {}
