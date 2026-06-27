package storymagine.redacteur.coeur.domaine.orchestrator.evaluate;

import storymagine.commun.coeur.ports.LogPort;
import storymagine.redacteur.coeur.domaine.orchestrator.GenerationConfig;
import storymagine.redacteur.coeur.domaine.scenario.Chapter;
import storymagine.redacteur.coeur.domaine.scenario.Scenario;
import storymagine.redacteur.coeur.domaine.story.Story;

/** Post-production phase: compresses the story summary after each chapter is validated. */
public class EvaluateWorkflow {

    private final StoryCompressorStep storyCompressorStep;
    private final LogPort             log;

    public EvaluateWorkflow(StoryCompressorStep storyCompressorStep, LogPort log) {
        this.storyCompressorStep = storyCompressorStep;
        this.log                 = log;
    }

    /** Compresses the story and updates the current chapter summary. */
    public void run(Scenario scenario, Chapter chapter, Story story, GenerationConfig config) {
        log.phaseHeader("POST-PROD", null);

        long t0            = System.nanoTime();
        var  compressorOut = storyCompressorStep.run(story);
        story.currentChapter().orElseThrow().setSummary(compressorOut.summary());
        log.step("StoryCompressor", ms(t0), null);
    }

    private static long ms(long nanoStart) {
        return (System.nanoTime() - nanoStart) / 1_000_000L;
    }
}
