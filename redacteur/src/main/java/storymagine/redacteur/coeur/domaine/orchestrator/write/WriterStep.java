package storymagine.redacteur.coeur.domaine.orchestrator.write;

import storymagine.redacteur.coeur.domaine.agent.sequence.writer.Writer;
import storymagine.redacteur.coeur.domaine.agent.sequence.writer.WriterInput;
import storymagine.redacteur.coeur.domaine.agent.sequence.writer.WriterOutput;
import storymagine.redacteur.coeur.domaine.orchestrator.common.ScenarioFormatters;
import storymagine.redacteur.coeur.domaine.orchestrator.common.StoryFormatters;
import storymagine.redacteur.coeur.domaine.scenario.Chapter;
import storymagine.redacteur.coeur.domaine.scenario.Scenario;
import storymagine.redacteur.coeur.domaine.scenario.Sequence;
import storymagine.redacteur.coeur.domaine.story.Story;
import storymagine.redacteur.coeur.domaine.story.WrittenChapter;
import storymagine.redacteur.coeur.domaine.story.WrittenSequence;

import java.util.ArrayList;
import java.util.List;

/** Activates Writer for one sequence. Pass rewriteProblems=null for the first attempt. */
public class WriterStep {

    private final Writer agent;

    public WriterStep(Writer agent) {
        this.agent = agent;
    }

    public WriterOutput run(Scenario scenario, Chapter chapter, Sequence sequence, Story story,
                            String sequencePlan, String rewriteProblems) {
        boolean isRewrite = rewriteProblems != null;
        WrittenChapter wc = story.currentChapter().orElseThrow();

        String stitch = sequence.overrides().stitch() != null ? sequence.overrides().stitch() : scenario.config().stitch();

        return agent.call(new WriterInput(
                sequence.directive(),
                scenario.resolveSequenceWords(sequence, chapter),
                isRewrite,
                ScenarioFormatters.personnages(merge(chapter.defaults().characters(), sequence.additions().characters()), true),
                ScenarioFormatters.focusText(merge(chapter.defaults().focus(), sequence.additions().focus()), true),
                ScenarioFormatters.loreText(merge(chapter.defaults().lore(), sequence.additions().lore()), true),
                rewriteProblems,
                previousSequenceText(wc),
                StoryFormatters.entityState(story.worldState()),
                story.summary(),
                sequencePlan,
                story.repetitionMemory().forbiddenPhrases(),
                story.repetitionMemory().forbiddenThemes(),
                ScenarioFormatters.writerConstraints(scenario, chapter, sequence),
                scenario.writingStyle(),
                scenario.writingExample(),
                chapter.setting(),
                stitch
        ));
    }

    private static <T> List<T> merge(List<T> base, List<T> extra) {
        if (extra.isEmpty()) return base;
        List<T> result = new ArrayList<>(base);
        result.addAll(extra);
        return result;
    }

    /** Full raw text of the last written sequence in this chapter — untruncated, "" if none yet. */
    private static String previousSequenceText(WrittenChapter wc) {
        List<WrittenSequence> seqs = wc.sequences();
        if (seqs.isEmpty()) return "";
        WrittenSequence last = seqs.get(seqs.size() - 1);
        return last.hasText() ? last.text() : "";
    }
}
