package storymagine.redacteur.coeur.domaine.agent.plan.chapterplanner;

import java.util.List;

/**
 * Input for ChapterPlanner — all strings are pre-formatted by the service layer.
 *
 * @param jsonMode       produce JSON array output (one object per sequence)
 * @param isRewrite      this is a revision run (feedback already in coherence)
 * @param previousPlan   non-null in correction mode: the plan to fix
 * @param problemsToFix  issues listed for the LLM to address (avoid or correct)
 */
public record ChapterPlannerInput(
    String chapterTitle,
    String chapterDescription,
    String chapterSetting,
    List<String> sequenceDescriptions,
    String bookGoal,
    String storySoFar,
    String entityState,
    String characters,
    String constraints,
    String focusText,
    String loreText,
    String coherence,
    List<String> forbiddenPhrases,
    int plannerEffortInLines,
    boolean jsonMode,
    boolean isRewrite,
    String previousPlan,
    String problemsToFix
) {}
