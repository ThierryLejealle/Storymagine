package storymagine.redacteur.coeur.domaine.agent.plan.chapterplanner;

import java.util.List;

/**
 * Output of ChapterPlanner.
 *
 * @param fullPlan       raw LLM text (free-form plan or JSON array)
 * @param sequencePlans  per-sequence formatted strings for the Writer; empty when not in jsonMode
 */
public record ChapterPlannerOutput(String fullPlan, List<String> sequencePlans) {
    public boolean isStructured() { return !sequencePlans.isEmpty(); }
}
