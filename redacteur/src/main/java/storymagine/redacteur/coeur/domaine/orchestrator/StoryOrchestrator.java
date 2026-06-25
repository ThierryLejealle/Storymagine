package storymagine.redacteur.coeur.domaine.orchestrator;

import storymagine.commun.coeur.ports.LogPort;
import storymagine.redacteur.coeur.domaine.orchestrator.evaluate.EvaluateWorkflow;
import storymagine.redacteur.coeur.domaine.orchestrator.plan.PlanWorkflow;
import storymagine.redacteur.coeur.domaine.orchestrator.write.WriteWorkflow;
import storymagine.redacteur.coeur.domaine.scenario.Chapter;
import storymagine.redacteur.coeur.domaine.scenario.Scenario;
import storymagine.redacteur.coeur.domaine.story.ChapterId;
import storymagine.redacteur.coeur.domaine.story.Story;
import storymagine.redacteur.coeur.ports.HtmlExportPort;

import java.util.List;

/**
 * Entry point for story generation.
 * Iterates over Scenario chapters and delegates each phase to the three workflows.
 */
public class StoryOrchestrator {

    private final PlanWorkflow     planWorkflow;
    private final WriteWorkflow    writeWorkflow;
    private final EvaluateWorkflow evaluateWorkflow;
    private final HtmlExportPort   htmlExport;
    private final LogPort          log;

    public StoryOrchestrator(PlanWorkflow planWorkflow,
                             WriteWorkflow writeWorkflow,
                             EvaluateWorkflow evaluateWorkflow,
                             HtmlExportPort htmlExport,
                             LogPort log) {
        this.planWorkflow     = planWorkflow;
        this.writeWorkflow    = writeWorkflow;
        this.evaluateWorkflow = evaluateWorkflow;
        this.htmlExport       = htmlExport;
        this.log              = log;
    }

    public Story generate(Scenario scenario, GenerationConfig config) {
        Story story = new Story();
        story.repetitionMemory().calibrate(scenario.config().contextWindow());

        List<Chapter> chapters = scenario.chapters();
        log.phaseHeader("SCENARIO", scenario.config().title() + " — " + chapters.size() + " chapitre(s)");

        for (int i = 0; i < chapters.size(); i++) {
            Chapter chapter = chapters.get(i);
            story.startChapter(new ChapterId(chapter.title()));
            log.phaseHeader("Chapitre " + (i + 1) + "/" + chapters.size(), chapter.title());

            planWorkflow.run(scenario, chapter, story, config);

            if (config.qualityLevel().runsWriting()) {
                writeWorkflow.run(scenario, chapter, story, config);
            }

            evaluateWorkflow.run(scenario, chapter, story, config);
            htmlExport.exportHtml(scenario.config().title(), story);
        }

        log.sessionEnd();
        return story;
    }
}
