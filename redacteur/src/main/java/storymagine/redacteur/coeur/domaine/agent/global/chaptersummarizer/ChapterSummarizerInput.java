package storymagine.redacteur.coeur.domaine.agent.global.chaptersummarizer;

/**
 * Input for ChapterSummarizer.
 *
 * @param chapterText full prose of the just-completed chapter
 * @param divisor     current per-chapter word budget divisor (Story.chapterSummaryDivisor) —
 *                    target words = word count of chapterText / divisor
 */
public record ChapterSummarizerInput(
    String chapterText,
    int divisor
) {}
