package storymagine.redacteur.coeur.domaine.agent.sequence.writer;

import java.util.List;

/**
 * All content pre-assembled by the service layer before calling Writer.
 * Corresponds to SequencePackage + write parameters from WriterContext.
 *
 * @param isRewrite       true when rewriting a rejected sequence (problems injected in chapterPlan)
 * @param sequencePlan    optional per-sequence plan slice from ChapterPlanner; overrides sequenceDescription
 * @param loopJournal     numbered one-liners from loop iterations (empty for IMPERATIVE chapters)
 * @param stitch          opening-transition rule; null means use built-in default
 */
public record WriterInput(
    String sequenceDescription,
    int minWords,
    boolean isRewrite,
    String charactersText,
    String focusText,
    String loreText,
    String actionsText,
    String chapterPlan,
    String prevSentences,
    String entityState,
    String storySoFar,
    String sequencePlan,
    List<String> forbiddenPhrases,
    List<String> forbiddenThemes,
    String loopJournal,
    String redactionConstraints,
    String styleGuide,
    String writingExample,
    String sequenceContext,
    String setting,
    String stitch
) {}
