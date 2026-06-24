package storymagine.redacteur.coeur.domaine.orchestrator.evaluate;

import storymagine.redacteur.coeur.domaine.agent.global.storycompressor.StoryCompressor;
import storymagine.redacteur.coeur.domaine.agent.global.storycompressor.StoryCompressorInput;
import storymagine.redacteur.coeur.domaine.agent.global.storycompressor.StoryCompressorOutput;
import storymagine.redacteur.coeur.domaine.story.Story;

/** Activates StoryCompressor to produce a running summary after a chapter is written. */
public class StoryCompressorStep {

    private static final int BASE_WORDS        = 150;
    private static final int WORDS_PER_CHAPTER = 30;

    private final StoryCompressor agent;

    public StoryCompressorStep(StoryCompressor agent) {
        this.agent = agent;
    }

    public StoryCompressorOutput run(Story story) {
        int chapterIndex = story.chapters().size() - 1;
        return agent.call(new StoryCompressorInput(
                story.storySoFar(),
                story.currentChapter().orElseThrow().fullText(),
                chapterIndex,
                BASE_WORDS,
                WORDS_PER_CHAPTER
        ));
    }
}
