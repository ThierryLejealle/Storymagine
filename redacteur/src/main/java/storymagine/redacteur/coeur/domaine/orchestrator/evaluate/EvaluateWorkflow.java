package storymagine.redacteur.coeur.domaine.orchestrator.evaluate;

import storymagine.commun.coeur.ports.LogPort;
import storymagine.redacteur.coeur.domaine.orchestrator.GenerationConfig;
import storymagine.redacteur.coeur.domaine.scenario.Chapter;
import storymagine.redacteur.coeur.domaine.scenario.Scenario;
import storymagine.redacteur.coeur.domaine.story.Story;

/** Post-production phase: summarizes the chapter into the running story summary after validation. */
public class EvaluateWorkflow {

    private final ChapterSummaryStep chapterSummaryStep;
    private final LogPort            log;

    public EvaluateWorkflow(ChapterSummaryStep chapterSummaryStep, LogPort log) {
        this.chapterSummaryStep = chapterSummaryStep;
        this.log                = log;
    }

    /** Appends the chapter summary to the story, compressing it first if it has grown past budget. */
    public void run(Scenario scenario, Chapter chapter, Story story, GenerationConfig config) {
        log.phaseHeader("POST-PROD", null);

        long t0 = System.nanoTime();
        ChapterSummaryResult result = chapterSummaryStep.run(story);

        int appendDelta = result.wordsAfterAppend() - result.wordsBefore();
        log.step("ChapterSummarizer", ms(t0),
                result.wordsBefore() + " -> " + result.wordsAfterAppend() + " mots (+" + appendDelta + ")");

        log.step("SequenceStateExtractor (reseed chapitre)", ms(t0),
                result.stateReseeded() ? "-> etat reinitialise et reensemence" : "-> etat reinitialise, aucun changement");

        if (result.compressed()) {
            log.warn("Résumé : seuil dépassé (" + result.wordsAfterAppend() + " > " + result.threshold()
                    + " mots) — compaction déclenchée");
            int compressDelta = result.wordsFinal() - result.wordsAfterAppend();
            log.step("SummaryCompressor", ms(t0),
                    result.wordsAfterAppend() + " -> " + result.wordsFinal() + " mots (" + compressDelta + ")");
        }
    }

    private static long ms(long nanoStart) {
        return (System.nanoTime() - nanoStart) / 1_000_000L;
    }
}
