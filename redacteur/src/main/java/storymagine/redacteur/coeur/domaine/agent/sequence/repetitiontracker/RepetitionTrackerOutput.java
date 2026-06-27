package storymagine.redacteur.coeur.domaine.agent.sequence.repetitiontracker;

import java.util.List;

/** Output of RepetitionTracker — new phrases and narrative patterns to forbid. */
public record RepetitionTrackerOutput(List<String> phrases, List<String> themes) {}
