package storymagine.redacteur.coeur.domaine.orchestrator.evaluate;

import org.junit.jupiter.api.Test;
import storymagine.commun.coeur.ports.LogPort;
import storymagine.redacteur.coeur.domaine.agent.global.chaptersummarizer.ChapterSummarizer;
import storymagine.redacteur.coeur.domaine.agent.global.summarycompressor.SummaryCompressor;
import storymagine.redacteur.coeur.domaine.agent.sequence.stateextractor.StateExtractor;
import storymagine.redacteur.coeur.domaine.orchestrator.CapturingLogPort;
import storymagine.redacteur.coeur.domaine.orchestrator.MockModelCallPort;
import storymagine.redacteur.coeur.domaine.orchestrator.write.StateExtractorStep;
import storymagine.redacteur.coeur.domaine.story.ChapterId;
import storymagine.redacteur.coeur.domaine.story.Story;
import storymagine.redacteur.coeur.domaine.story.WrittenSequence;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests ChapterSummaryStep's WorldState reset+reseed at chapter boundaries — added 2026-07-12
 * alongside the fix for unbounded entityStates growth (see evols). Confirms entity state is
 * cleared before each chapter transition and reseeded from the fresh chapter summary alone,
 * never carried forward or merged into Story.summary() prose.
 */
class ChapterSummaryStepTest {

    private static final String CHAPTER_SUMMARIZER_FRAGMENT = "résumé factuel compact d'UN chapitre";
    private static final String STATE_EXTRACTOR_FRAGMENT    = "tracker d'état narratif";

    private static Story storyWithOneWrittenChapter() {
        Story story = new Story();
        story.startChapter(new ChapterId("ch1"));
        story.currentChapter().orElseThrow().addSequence(new WrittenSequence(null, "Un texte de sequence."));
        return story;
    }

    private static ChapterSummaryStep buildStep(MockModelCallPort llm) {
        LogPort log = new CapturingLogPort();
        var chapterSummarizer  = new ChapterSummarizer(llm, log);
        var summaryCompressor  = new SummaryCompressor(llm);
        var stateExtractorStep = new StateExtractorStep(new StateExtractor(llm, log));
        return new ChapterSummaryStep(chapterSummarizer, summaryCompressor, stateExtractorStep);
    }

    @Test
    void chapterTransition_clearsPreviousEntityState_beforeReseeding() {
        MockModelCallPort llm = MockModelCallPort.builder(8000)
                .when(CHAPTER_SUMMARIZER_FRAGMENT, "Le groupe arrive au village.")
                .when(STATE_EXTRACTOR_FRAGMENT, "ETAT: Thierry → arrivé au village")
                .otherwise("")
                .build();
        Story story = storyWithOneWrittenChapter();
        story.worldState().updateEntities(java.util.Map.of("Thierry", "etat perime d'un chapitre precedent"));

        buildStep(llm).run(story);

        assertEquals("arrivé au village", story.worldState().entityStates().get("Thierry"),
                "The stale entry from before the chapter must be replaced, not kept alongside the new one");
    }

    @Test
    void chapterSummaryText_neverContainsEntityStateLines() {
        MockModelCallPort llm = MockModelCallPort.builder(8000)
                .when(CHAPTER_SUMMARIZER_FRAGMENT, "Le groupe arrive au village.")
                .when(STATE_EXTRACTOR_FRAGMENT, "ETAT: Thierry → arrivé au village")
                .otherwise("")
                .build();
        Story story = storyWithOneWrittenChapter();

        buildStep(llm).run(story);

        assertEquals("Le groupe arrive au village.", story.summary(),
                "Entity state must live only in WorldState, never appended into the narrative summary");
    }

    @Test
    void noChangesReported_leavesEntityStatesEmptyAfterClear() {
        MockModelCallPort llm = MockModelCallPort.builder(8000)
                .when(CHAPTER_SUMMARIZER_FRAGMENT, "Le groupe arrive au village.")
                .when(STATE_EXTRACTOR_FRAGMENT, "AUCUN")
                .otherwise("")
                .build();
        Story story = storyWithOneWrittenChapter();
        story.worldState().updateEntities(java.util.Map.of("Thierry", "etat perime"));

        ChapterSummaryResult result = buildStep(llm).run(story);

        assertTrue(story.worldState().entityStates().isEmpty());
        assertFalse(result.stateReseeded());
    }

    @Test
    void stateReseeded_reflectsWhetherStateExtractorReportedChanges() {
        MockModelCallPort llm = MockModelCallPort.builder(8000)
                .when(CHAPTER_SUMMARIZER_FRAGMENT, "Le groupe arrive au village.")
                .when(STATE_EXTRACTOR_FRAGMENT, "ETAT: Thierry → arrivé au village")
                .otherwise("")
                .build();
        Story story = storyWithOneWrittenChapter();

        ChapterSummaryResult result = buildStep(llm).run(story);

        assertTrue(result.stateReseeded());
    }
}
