package storymagine.redacteur.coeur.domaine.agent.chapter.coherencecritic;

/** Input for ChapterCoherenceCritic — checks factual coherence of a chapter text. */
public record ChapterCoherenceCriticInput(
    String text,
    String checks
) {}
