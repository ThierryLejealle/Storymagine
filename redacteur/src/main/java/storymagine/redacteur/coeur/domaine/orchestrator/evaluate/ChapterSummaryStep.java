package storymagine.redacteur.coeur.domaine.orchestrator.evaluate;

import storymagine.commun.coeur.domaine.text.SummaryBudget;
import storymagine.redacteur.coeur.domaine.agent.global.chaptersummarizer.ChapterSummarizer;
import storymagine.redacteur.coeur.domaine.agent.global.chaptersummarizer.ChapterSummarizerInput;
import storymagine.redacteur.coeur.domaine.agent.global.summarycompressor.SummaryCompressor;
import storymagine.redacteur.coeur.domaine.agent.global.summarycompressor.SummaryCompressorInput;
import storymagine.redacteur.coeur.domaine.agent.sequence.stateextractor.StateExtractorOutput;
import storymagine.redacteur.coeur.domaine.orchestrator.common.StoryFormatters;
import storymagine.redacteur.coeur.domaine.orchestrator.write.StateExtractorStep;
import storymagine.redacteur.coeur.domaine.story.Story;

/**
 * Summarizes the just-completed chapter into the running story summary, then compresses
 * that summary if it has grown past SummaryBudget.wordBudget() — see ChapterSummarizer.md
 * and SummaryCompressor.md for the rationale.
 *
 * Also resets and reseeds WorldState's entity state at this same chapter boundary: the running
 * narrative summary and the entity state are two separate concepts (one is a history, the other
 * is a snapshot) and must never be merged — see StoryFormatters.entityState vs Story.summary.
 * Entity state accumulated sequence-by-sequence during the chapter is cleared, then reseeded in
 * one pass from the freshly produced (already curated) chapter summary — otherwise entityStates
 * would grow forever across the whole book instead of staying a bounded, always-current snapshot.
 */
public class ChapterSummaryStep {

    private final ChapterSummarizer  chapterSummarizer;
    private final SummaryCompressor  summaryCompressor;
    private final StateExtractorStep stateExtractorStep;

    public ChapterSummaryStep(ChapterSummarizer chapterSummarizer, SummaryCompressor summaryCompressor,
                              StateExtractorStep stateExtractorStep) {
        this.chapterSummarizer  = chapterSummarizer;
        this.summaryCompressor  = summaryCompressor;
        this.stateExtractorStep = stateExtractorStep;
    }

    public ChapterSummaryResult run(Story story) {
        int wordsBefore = wordCount(story.summary());

        String chapterText = story.currentChapter().orElseThrow().fullText();
        String chapterSummary = chapterSummarizer.call(
                new ChapterSummarizerInput(chapterText, story.chapterSummaryDivisor())).summary();
        story.appendChapterSummary(chapterSummary);
        int wordsAfterAppend = wordCount(story.summary());

        story.worldState().clearExtractedState();
        StateExtractorOutput stateOut = stateExtractorStep.run(chapterSummary, story.worldState());
        boolean stateReseeded = stateOut.hasChanges();
        if (stateReseeded) StoryFormatters.applyStateLines(story.worldState(), stateOut.stateLines());

        int threshold = SummaryBudget.wordBudget(chapterSummarizer.contextWindow());
        if (wordsAfterAppend > threshold) {
            String compressed = summaryCompressor.call(new SummaryCompressorInput(story.summary())).summary();
            story.compressSummary(compressed);
            return new ChapterSummaryResult(wordsBefore, wordsAfterAppend, threshold, true, wordCount(story.summary()), stateReseeded);
        }
        return new ChapterSummaryResult(wordsBefore, wordsAfterAppend, threshold, false, wordsAfterAppend, stateReseeded);
    }

    private static int wordCount(String text) {
        return text == null || text.isBlank() ? 0 : text.trim().split("\\s+").length;
    }
}
