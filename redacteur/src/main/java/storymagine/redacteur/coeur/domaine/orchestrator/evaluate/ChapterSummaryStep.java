package storymagine.redacteur.coeur.domaine.orchestrator.evaluate;

import storymagine.commun.coeur.domaine.text.SummaryBudget;
import storymagine.redacteur.coeur.domaine.agent.global.chaptersummarizer.ChapterSummarizer;
import storymagine.redacteur.coeur.domaine.agent.global.chaptersummarizer.ChapterSummarizerInput;
import storymagine.redacteur.coeur.domaine.agent.global.summarycompressor.SummaryCompressor;
import storymagine.redacteur.coeur.domaine.agent.global.summarycompressor.SummaryCompressorInput;
import storymagine.redacteur.coeur.domaine.story.Story;

/**
 * Summarizes the just-completed chapter into the running story summary, then compresses
 * that summary if it has grown past SummaryBudget.wordBudget() — see ChapterSummarizer.md
 * and SummaryCompressor.md for the rationale.
 */
public class ChapterSummaryStep {

    private final ChapterSummarizer chapterSummarizer;
    private final SummaryCompressor summaryCompressor;

    public ChapterSummaryStep(ChapterSummarizer chapterSummarizer, SummaryCompressor summaryCompressor) {
        this.chapterSummarizer = chapterSummarizer;
        this.summaryCompressor = summaryCompressor;
    }

    public ChapterSummaryResult run(Story story) {
        int wordsBefore = wordCount(story.summary());

        String chapterText = story.currentChapter().orElseThrow().fullText();
        String chapterSummary = chapterSummarizer.call(
                new ChapterSummarizerInput(chapterText, story.chapterSummaryDivisor())).summary();
        story.appendChapterSummary(chapterSummary);
        int wordsAfterAppend = wordCount(story.summary());

        int threshold = SummaryBudget.wordBudget(chapterSummarizer.contextWindow());
        if (wordsAfterAppend > threshold) {
            String compressed = summaryCompressor.call(new SummaryCompressorInput(story.summary())).summary();
            story.compressSummary(compressed);
            return new ChapterSummaryResult(wordsBefore, wordsAfterAppend, threshold, true, wordCount(story.summary()));
        }
        return new ChapterSummaryResult(wordsBefore, wordsAfterAppend, threshold, false, wordsAfterAppend);
    }

    private static int wordCount(String text) {
        return text == null || text.isBlank() ? 0 : text.trim().split("\\s+").length;
    }
}
