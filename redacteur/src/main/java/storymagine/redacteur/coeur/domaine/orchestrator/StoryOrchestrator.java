package storymagine.redacteur.coeur.domaine.orchestrator;

import storymagine.commun.coeur.ports.LogPort;
import storymagine.redacteur.coeur.domaine.orchestrator.evaluate.EvaluateWorkflow;
import storymagine.redacteur.coeur.domaine.orchestrator.storyplan.StoryPlanWorkflow;
import storymagine.redacteur.coeur.domaine.orchestrator.write.WriteWorkflow;
import storymagine.redacteur.coeur.domaine.scenario.Chapter;
import storymagine.redacteur.coeur.domaine.scenario.Scenario;
import storymagine.redacteur.coeur.domaine.story.ChapterId;
import storymagine.redacteur.coeur.domaine.story.Story;
import storymagine.redacteur.coeur.ports.CheckpointPort;
import storymagine.redacteur.coeur.ports.HtmlExportPort;

import java.nio.file.Path;
import java.util.List;

/**
 * Entry point for story generation.
 * Plans the whole book first (StoryPlanWorkflow), then writes and evaluates chapter by chapter.
 * A checkpoint is saved after each completed chapter, so an interrupted run can be resumed
 * (see resume()) instead of restarting from the first chapter.
 */
public class StoryOrchestrator {

    private final StoryPlanWorkflow storyPlanWorkflow;
    private final WriteWorkflow     writeWorkflow;
    private final EvaluateWorkflow  evaluateWorkflow;
    private final HtmlExportPort    htmlExport;
    private final CheckpointPort    checkpoint;
    private final LogPort           log;

    public StoryOrchestrator(StoryPlanWorkflow storyPlanWorkflow,
                             WriteWorkflow writeWorkflow,
                             EvaluateWorkflow evaluateWorkflow,
                             HtmlExportPort htmlExport,
                             CheckpointPort checkpoint,
                             LogPort log) {
        this.storyPlanWorkflow = storyPlanWorkflow;
        this.writeWorkflow     = writeWorkflow;
        this.evaluateWorkflow  = evaluateWorkflow;
        this.htmlExport        = htmlExport;
        this.checkpoint        = checkpoint;
        this.log               = log;
    }

    public Story generate(Scenario scenario, GenerationConfig config) {
        Story story = new Story();
        story.repetitionMemory().calibrate(scenario.config().contextWindow());

        log.phaseHeader("SCENARIO",
            scenario.config().title() + " — " + scenario.chapters().size() + " chapitre(s)");

        log.phaseHeader("PLAN", null);
        storyPlanWorkflow.run(scenario, story, config);

        return writeAndEvaluate(scenario, config, story, 0);
    }

    /** Resumes an interrupted generation from its last saved checkpoint, skipping the plan phase. */
    public Story resume(Scenario scenario, Path runDir) {
        GenerationCheckpoint saved = checkpoint.load(runDir)
            .orElseThrow(() -> new IllegalStateException("Aucun checkpoint trouve dans " + runDir));
        Story story = Story.restore(saved.story());

        log.phaseHeader("SCENARIO", scenario.config().title() + " — reprise au chapitre "
            + (saved.nextChapterIndex() + 1) + "/" + scenario.chapters().size());

        return writeAndEvaluate(scenario, saved.config(), story, saved.nextChapterIndex());
    }

    private Story writeAndEvaluate(Scenario scenario, GenerationConfig config, Story story, int fromChapterIndex) {
        List<Chapter> chapters = scenario.chapters();
        boolean       runsWriting = config.qualityLevel().runsWriting();
        if (runsWriting) {
            log.phaseHeader("WRITE", null);
        }
        for (int i = fromChapterIndex; i < chapters.size(); i++) {
            Chapter chapter = chapters.get(i);
            story.activateChapter(new ChapterId(chapter.title()));

            if (runsWriting) {
                log.phaseHeader("WRITE chapitre " + (i + 1) + "/" + chapters.size(), chapter.title());
                writeWorkflow.run(scenario, chapter, story, config);
            }

            evaluateWorkflow.run(scenario, chapter, story, config);
            htmlExport.exportHtml(scenario.config().title(), story);
            checkpoint.save(new GenerationCheckpoint(story.snapshot(), i + 1, config));
        }

        checkpoint.clear();
        log.sessionEnd();
        return story;
    }
}
