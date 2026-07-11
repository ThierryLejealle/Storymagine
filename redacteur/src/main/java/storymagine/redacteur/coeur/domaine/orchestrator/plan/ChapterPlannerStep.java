package storymagine.redacteur.coeur.domaine.orchestrator.plan;

import storymagine.redacteur.coeur.domaine.agent.plan.chapterplanner.ChapterPlanner;
import storymagine.redacteur.coeur.domaine.agent.plan.chapterplanner.ChapterPlannerInput;
import storymagine.redacteur.coeur.domaine.agent.plan.chapterplanner.ChapterPlannerOutput;
import storymagine.redacteur.coeur.domaine.orchestrator.common.ScenarioFormatters;
import storymagine.redacteur.coeur.domaine.orchestrator.common.StoryFormatters;
import storymagine.redacteur.coeur.domaine.scenario.Chapter;
import storymagine.redacteur.coeur.domaine.scenario.Scenario;
import storymagine.redacteur.coeur.domaine.scenario.Sequence;
import storymagine.redacteur.coeur.domaine.story.Story;
import storymagine.redacteur.coeur.domaine.story.WrittenChapter;

import java.util.ArrayList;
import java.util.List;

/** Activates ChapterPlanner: reads Scenario + Story, builds ChapterPlannerInput, returns output. */
public class ChapterPlannerStep {

    private final ChapterPlanner agent;
    private final BeatsConfig    beatsConfig;

    public ChapterPlannerStep(ChapterPlanner agent, BeatsConfig beatsConfig) {
        this.agent       = agent;
        this.beatsConfig = beatsConfig;
    }

    public ChapterPlannerOutput run(Scenario scenario, Chapter chapter, Story story, boolean jsonMode) {
        WrittenChapter wc        = story.currentChapter().orElseThrow();
        boolean        isRewrite = wc.plan() != null;

        List<String> seqDescriptions = jsonMode
                ? annotatedSequenceDescriptions(scenario, chapter)
                : ScenarioFormatters.sequenceDescriptions(chapter.sequences());

        return agent.call(new ChapterPlannerInput(
                chapter.title(),
                chapter.description(),
                chapter.setting(),
                seqDescriptions,
                ScenarioFormatters.bookGoal(scenario),
                story.summary(),
                StoryFormatters.entityState(story.worldState()),
                ScenarioFormatters.personnages(chapter.defaults().characters(), false),
                ScenarioFormatters.planConstraints(scenario, chapter),
                ScenarioFormatters.focusText(chapter.defaults().focus(), false),
                ScenarioFormatters.loreText(chapter.defaults().lore(), false),
                story.repetitionMemory().forbiddenPhrases(),
                jsonMode,
                isRewrite,
                isRewrite ? wc.plan() : null,
                wc.coherence()
        ));
    }

    /** Builds sequence descriptions annotated with per-sequence beats range derived from word count. */
    private List<String> annotatedSequenceDescriptions(Scenario scenario, Chapter chapter) {
        List<Sequence> sequences = chapter.sequences();
        List<String>   result    = new ArrayList<>(sequences.size());
        for (Sequence seq : sequences) {
            String base        = ScenarioFormatters.singleSequenceDescription(seq);
            String beatsHint   = beatsHint(scenario.resolveSequenceWords(seq, chapter));
            result.add(base.isBlank() ? beatsHint : base + "\n" + beatsHint);
        }
        return result;
    }

    private String beatsHint(int targetWords) {
        int targetBeats = beatsConfig.beatsBase() + Math.max(1, Math.round((float) targetWords / beatsConfig.wordsPerBeat()));
        int minBeats    = Math.max(1, Math.round(targetBeats * (100 - beatsConfig.tolerancePct()) / 100.0f));
        int maxBeats    = Math.round(targetBeats * (100 + beatsConfig.tolerancePct()) / 100.0f);
        return "[Nombre de beats : " + minBeats + " a " + maxBeats + "]";
    }
}
