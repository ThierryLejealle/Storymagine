package storymagine.redacteur.coeur.domaine.orchestrator.evaluate;

/**
 * Result of ChapterSummaryStep — word counts for logging (size before/after the chapter
 * summary was appended, and after compaction if it ran).
 *
 * @param wordsBefore     Story.summary() word count before this chapter's summary was appended
 * @param wordsAfterAppend word count right after appending, before any compaction
 * @param threshold       SummaryBudget.wordBudget() used for the compaction check
 * @param compressed      true if SummaryCompressor ran
 * @param wordsFinal      word count after compaction (== wordsAfterAppend if not compressed)
 * @param stateReseeded   true if WorldState's entity state was cleared and reseeded from the
 *                        fresh chapter summary (StateExtractor reported at least one change)
 */
public record ChapterSummaryResult(
    int wordsBefore,
    int wordsAfterAppend,
    int threshold,
    boolean compressed,
    int wordsFinal,
    boolean stateReseeded
) {}
