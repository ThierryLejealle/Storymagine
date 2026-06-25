package storymagine.redacteur.coeur.domaine.orchestrator.evaluate;

import storymagine.commun.coeur.ports.LogPort;
import storymagine.redacteur.coeur.domaine.orchestrator.GenerationConfig;
import storymagine.redacteur.coeur.domaine.scenario.Chapter;
import storymagine.redacteur.coeur.domaine.scenario.Scenario;
import storymagine.redacteur.coeur.domaine.story.Story;

/**
 * Orchestrates the evaluation phase at the end of each chapter:
 * StoryCompressor → ChapterStyleChecker → CharacterChecker → NarrativeArcAnalyzer → CausalAnalyzer.
 * Only runs when GenerationConfig.mode is FULL.
 */
public class EvaluateWorkflow {

    private final StoryCompressorStep      storyCompressorStep;
    private final ChapterStyleCheckerStep  chapterStyleCheckerStep;
    private final CharacterCheckerStep     characterCheckerStep;
    private final NarrativeArcAnalyzerStep narrativeArcAnalyzerStep;
    private final CausalAnalyzerStep       causalAnalyzerStep;
    private final LogPort                  log;

    public EvaluateWorkflow(StoryCompressorStep storyCompressorStep,
                            ChapterStyleCheckerStep chapterStyleCheckerStep,
                            CharacterCheckerStep characterCheckerStep,
                            NarrativeArcAnalyzerStep narrativeArcAnalyzerStep,
                            CausalAnalyzerStep causalAnalyzerStep,
                            LogPort log) {
        this.storyCompressorStep      = storyCompressorStep;
        this.chapterStyleCheckerStep  = chapterStyleCheckerStep;
        this.characterCheckerStep     = characterCheckerStep;
        this.narrativeArcAnalyzerStep = narrativeArcAnalyzerStep;
        this.causalAnalyzerStep       = causalAnalyzerStep;
        this.log                      = log;
    }

    /** Evaluates the current chapter and updates Story with its summary. */
    public void run(Scenario scenario, Chapter chapter, Story story, GenerationConfig config) {
        log.phaseHeader("EVAL", null);

        long t0 = System.nanoTime();
        var compressorOut = storyCompressorStep.run(story);
        story.currentChapter().orElseThrow().setSummary(compressorOut.summary());
        log.step("StoryCompressor", ms(t0), null);

        if (!config.qualityLevel().runsEvaluation()) return;

        t0 = System.nanoTime();
        chapterStyleCheckerStep.run(story, scenario);
        log.step("ChapterStyleChecker", ms(t0), null);

        t0 = System.nanoTime();
        characterCheckerStep.run(story, scenario, chapter);
        log.step("CharacterChecker", ms(t0), null);

        t0 = System.nanoTime();
        narrativeArcAnalyzerStep.run(story);
        log.step("NarrativeArcAnalyzer", ms(t0), null);

        t0 = System.nanoTime();
        causalAnalyzerStep.run(story);
        log.step("CausalAnalyzer", ms(t0), null);
    }

    private static long ms(long nanoStart) {
        return (System.nanoTime() - nanoStart) / 1_000_000L;
    }
}
