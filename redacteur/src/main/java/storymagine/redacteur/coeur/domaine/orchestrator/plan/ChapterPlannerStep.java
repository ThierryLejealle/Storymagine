package storymagine.redacteur.coeur.domaine.orchestrator.plan;

import storymagine.redacteur.coeur.domaine.agent.plan.chapterplanner.ChapterPlanner;
import storymagine.redacteur.coeur.domaine.agent.plan.chapterplanner.ChapterPlannerInput;
import storymagine.redacteur.coeur.domaine.agent.plan.chapterplanner.ChapterPlannerOutput;
import storymagine.redacteur.coeur.domaine.orchestrator.common.ScenarioFormatters;
import storymagine.redacteur.coeur.domaine.orchestrator.common.StoryFormatters;
import storymagine.redacteur.coeur.domaine.scenario.Chapter;
import storymagine.redacteur.coeur.domaine.scenario.Scenario;
import storymagine.redacteur.coeur.domaine.story.Story;
import storymagine.redacteur.coeur.domaine.story.WrittenChapter;

/** Activates ChapterPlanner: reads Scenario + Story, builds ChapterPlannerInput, returns output. */
public class ChapterPlannerStep {

    private final ChapterPlanner agent;

    public ChapterPlannerStep(ChapterPlanner agent) {
        this.agent = agent;
    }

    public ChapterPlannerOutput run(Scenario scenario, Chapter chapter, Story story, boolean jsonMode) {
        WrittenChapter wc        = story.currentChapter().orElseThrow();
        boolean        isRewrite = wc.plan() != null;
        int            effort    = resolveEffort(scenario, chapter);

        return agent.call(new ChapterPlannerInput(
                chapter.title(),
                chapter.description(),
                chapter.setting(),
                ScenarioFormatters.sequenceDescriptions(chapter.sequences()),
                ScenarioFormatters.bookGoal(scenario),
                story.storySoFar(),
                StoryFormatters.entityState(story.worldState()),
                ScenarioFormatters.personnages(chapter.defaults().characters(), false),
                ScenarioFormatters.planConstraints(scenario, chapter),
                ScenarioFormatters.focusText(chapter.defaults().focus(), false),
                ScenarioFormatters.loreText(chapter.defaults().lore(), false),
                wc.coherence(),
                story.repetitionMemory().forbiddenPhrases(),
                effort,
                jsonMode,
                isRewrite,
                isRewrite ? wc.plan() : null,
                wc.coherence()
        ));
    }

    private int resolveEffort(Scenario scenario, Chapter chapter) {
        if (chapter.defaults().plannerEffortInLines() != null)
            return chapter.defaults().plannerEffortInLines();
        if (scenario.config().plannerEffortInLines() != null)
            return scenario.config().plannerEffortInLines();
        return ChapterPlanner.DEFAULT_PLANNER_EFFORT_IN_LINES;
    }
}
