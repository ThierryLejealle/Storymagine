package storymagine.redacteur.coeur.domaine.agent.global.storycompressor;

/**
 * Input for StoryCompressor.
 *
 * @param summaryBaseWords        base word target for the summary
 * @param summaryWordsPerChapter  words added per chapter (grows with book length)
 */
public record StoryCompressorInput(
    String existingSummary,
    String newChapterText,
    int chapterIndex,
    int summaryBaseWords,
    int summaryWordsPerChapter
) {}
