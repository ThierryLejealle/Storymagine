package storymagine.redacteur.coeur.domaine.agent.sequence.repetitionfilter;

import java.util.List;

/** Input for RepetitionFilter — candidates to ban, and leitmotiv content to protect. */
public record RepetitionFilterInput(List<String> candidates, String keepPhrasesContent) {}
