package storymagine.redacteur.coeur.domaine.agent.plan.chapterplanner;

import java.util.List;

/**
 * Input for ChapterPlanner — all strings are pre-formatted by the service layer.
 * sequenceDescriptions include beats range hints (e.g. "[Nombre de beats : 3 a 5]") when in JSON mode.
 *
 * @param jsonMode       produce JSON array output (one object per sequence)
 * @param isRewrite      this is a revision run (feedback already in problemsToFix)
 * @param previousPlan   non-null in correction mode: the plan to fix
 * @param problemsToFix  issues listed for the LLM to address (avoid or correct)
 */
public record ChapterPlannerInput(
    String chapterTitle,
    String chapterDescription,
    String chapterSetting,
    List<String> sequenceDescriptions,
    String bookGoal,
    String summary,
    String entityState,
    String characters,
    String constraints,
    String focusText,
    String loreText,
    List<String> forbiddenPhrases,
    boolean jsonMode,
    boolean isRewrite,
    String previousPlan,
    String problemsToFix
) {}
