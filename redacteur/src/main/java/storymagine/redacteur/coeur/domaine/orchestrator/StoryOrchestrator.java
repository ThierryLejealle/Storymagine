package storymagine.redacteur.coeur.domaine.orchestrator;

import storymagine.commun.coeur.ports.LogPort;
import storymagine.redacteur.coeur.domaine.orchestrator.evaluate.EvaluateWorkflow;
import storymagine.redacteur.coeur.domaine.orchestrator.storyplan.StoryPlanWorkflow;
import storymagine.redacteur.coeur.domaine.orchestrator.write.WriteWorkflow;
import storymagine.redacteur.coeur.domaine.scenario.Chapter;
import storymagine.redacteur.coeur.domaine.scenario.Scenario;
import storymagine.redacteur.coeur.domaine.story.ChapterId;
import storymagine.redacteur.coeur.domaine.story.Story;
import storymagine.redacteur.coeur.ports.HtmlExportPort;

import java.util.List;

/**
 * Entry point for story generation.
 * Plans the whole book first (StoryPlanWorkflow), then writes and evaluates chapter by chapter.
 */
public class StoryOrchestrator {

    private final StoryPlanWorkflow storyPlanWorkflow;
    private final WriteWorkflow     writeWorkflow;
    private final EvaluateWorkflow  evaluateWorkflow;
    private final HtmlExportPort    htmlExport;
    private final LogPort           log;

    public StoryOrchestrator(StoryPlanWorkflow storyPlanWorkflow,
                             WriteWorkflow writeWorkflow,
                             EvaluateWorkflow evaluateWorkflow,
                             HtmlExportPort htmlExport,
                             LogPort log) {
        this.storyPlanWorkflow = storyPlanWorkflow;
        this.writeWorkflow     = writeWorkflow;
        this.evaluateWorkflow  = evaluateWorkflow;
        this.htmlExport        = htmlExport;
        this.log               = log;
    }

    public Story generate(Scenario scenario, GenerationConfig config) {
        Story story = new Story();
        story.repetitionMemory().calibrate(scenario.config().contextWindow());

        List<Chapter> chapters = scenario.chapters();
        log.phaseHeader("SCENARIO", scenario.config().title() + " — " + chapters.size() + " chapitre(s)");

        log.phaseHeader("PLAN", null);
        storyPlanWorkflow.run(scenario, story, config);

        boolean runsWriting = config.qualityLevel().runsWriting();
        if (runsWriting) {
            log.phaseHeader("WRITE", null);
        }
        for (int i = 0; i < chapters.size(); i++) {
            Chapter chapter = chapters.get(i);
            story.activateChapter(new ChapterId(chapter.title()));

            if (runsWriting) {
                log.phaseHeader("WRITE chapitre " + (i + 1) + "/" + chapters.size(), chapter.title());
                writeWorkflow.run(scenario, chapter, story, config);
            }

            evaluateWorkflow.run(scenario, chapter, story, config);
            htmlExport.exportHtml(scenario.config().title(), story);
        }

        log.sessionEnd();
        return story;
    }
}
