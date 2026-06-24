package storymagine.redacteur.coeur.domaine.agent.writer.repetitiontracker;

import java.util.List;

/** Output of RepetitionTracker — new phrases and narrative patterns to forbid. */
public record RepetitionTrackerOutput(List<String> phrases, List<String> themes) {}
