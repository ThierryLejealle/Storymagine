package storymagine.redacteur.coeur.domaine.orchestrator;

import storymagine.redacteur.coeur.domaine.story.Story;

/**
 * Persisted state of an interrupted generation: the story written so far, the index of the next
 * chapter to write, and the profile the run was started with — enough to resume identically.
 */
public record GenerationCheckpoint(Story.Snapshot story, int nextChapterIndex, GenerationConfig config) {}
