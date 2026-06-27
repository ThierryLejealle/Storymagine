package storymagine.redacteur.coeur.domaine.agent.sequence.repetitiontracker;

import java.util.List;

/** Input for RepetitionTracker — text to scan + already-tracked items to avoid re-extracting. */
public record RepetitionTrackerInput(
    String text,
    List<String> alreadyPhrases,
    List<String> alreadyThemes
) {}
