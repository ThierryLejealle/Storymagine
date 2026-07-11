package storymagine.redacteur.coeur.domaine.agent.sequence.writer;

import java.util.List;

/**
 * All content pre-assembled by the service layer before calling Writer.
 * Corresponds to SequencePackage + write parameters from WriterContext.
 *
 * @param isRewrite            true when rewriting a rejected sequence (problems carried by rewriteProblems)
 * @param sequencePlan         optional per-sequence plan slice from ChapterPlanner; overrides sequenceDescription
 * @param rewriteProblems      problems to fix, non-null only when isRewrite — never the whole chapter plan,
 *                             so the Writer is never shown other sequences' beats (spoiler risk)
 * @param previousSequenceText full text of the last written sequence in this chapter, raw and untruncated;
 *                             null/blank if this is the chapter's first sequence
 * @param stitch               opening-transition rule; null means use built-in default
 */
public record WriterInput(
    String sequenceDescription,
    int minWords,
    boolean isRewrite,
    String charactersText,
    String focusText,
    String loreText,
    String rewriteProblems,
    String previousSequenceText,
    String entityState,
    String summary,
    String sequencePlan,
    List<String> forbiddenPhrases,
    List<String> forbiddenThemes,
    String redactionConstraints,
    String styleGuide,
    String writingExample,
    String setting,
    String stitch
) {}
