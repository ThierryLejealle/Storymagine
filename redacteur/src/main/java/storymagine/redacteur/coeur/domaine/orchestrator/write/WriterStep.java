package storymagine.redacteur.coeur.domaine.orchestrator.write;

import storymagine.redacteur.coeur.domaine.agent.writer.sequencewriter.Writer;
import storymagine.redacteur.coeur.domaine.agent.writer.sequencewriter.WriterInput;
import storymagine.redacteur.coeur.domaine.agent.writer.sequencewriter.WriterOutput;
import storymagine.redacteur.coeur.domaine.orchestrator.common.ScenarioFormatters;
import storymagine.redacteur.coeur.domaine.orchestrator.common.StoryFormatters;
import storymagine.redacteur.coeur.domaine.scenario.Chapter;
import storymagine.redacteur.coeur.domaine.scenario.Scenario;
import storymagine.redacteur.coeur.domaine.scenario.Sequence;
import storymagine.redacteur.coeur.domaine.story.Story;
import storymagine.redacteur.coeur.domaine.story.WrittenChapter;
import storymagine.redacteur.coeur.domaine.story.WrittenSequence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Activates Writer for one sequence. Pass rewriteProblems=null for the first attempt. */
public class WriterStep {

    private final Writer agent;

    public WriterStep(Writer agent) {
        this.agent = agent;
    }

    public WriterOutput run(Scenario scenario, Chapter chapter, Sequence sequence, Story story,
                            String sequencePlan, String actionsText, String rewriteProblems) {
        WrittenChapter wc        = story.currentChapter().orElseThrow();
        boolean        isRewrite = rewriteProblems != null;

        String chapterPlan = wc.plan();
        if (isRewrite) {
            chapterPlan = chapterPlan + "\n\n### Problèmes à corriger\n" + rewriteProblems;
        }

        String stitch = sequence.overrides().stitch() != null ? sequence.overrides().stitch() : scenario.config().stitch();

        return agent.call(new WriterInput(
                sequence.directive(),
                sequence.overrides().minWords(),
                isRewrite,
                ScenarioFormatters.personnages(merge(chapter.defaults().characters(), sequence.additions().characters()), true),
                ScenarioFormatters.focusText(merge(chapter.defaults().focus(), sequence.additions().focus()), true),
                ScenarioFormatters.loreText(merge(chapter.defaults().lore(), sequence.additions().lore()), true),
                actionsText,
                chapterPlan,
                lastSentences(wc, 3),
                StoryFormatters.entityState(story.worldState()),
                story.storySoFar(),
                sequencePlan,
                story.repetitionMemory().forbiddenPhrases(),
                story.repetitionMemory().forbiddenThemes(),
                null,
                ScenarioFormatters.writerConstraints(scenario, chapter, sequence),
                null,
                scenario.writingExample(),
                null,
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

    private static String lastSentences(WrittenChapter wc, int count) {
        List<WrittenSequence> seqs = wc.sequences();
        if (seqs.isEmpty()) return "";
        WrittenSequence last = seqs.get(seqs.size() - 1);
        if (!last.hasText()) return "";
        String[] sentences = last.text().split("(?<=[.!?])\\s+");
        int from = Math.max(0, sentences.length - count);
        return String.join(" ", Arrays.copyOfRange(sentences, from, sentences.length));
    }
}
