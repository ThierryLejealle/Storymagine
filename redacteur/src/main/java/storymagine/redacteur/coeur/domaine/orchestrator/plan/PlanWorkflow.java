package storymagine.redacteur.coeur.domaine.orchestrator.plan;

import storymagine.commun.coeur.ports.LogPort;
import storymagine.redacteur.coeur.domaine.agent.plan.goalplanchecker.GoalPlanCheckerOutput;
import storymagine.redacteur.coeur.domaine.agent.plan.plancoherencecritic.PlanCoherenceCriticOutput;
import storymagine.redacteur.coeur.domaine.agent.plan.plannarrativecritic.PlanNarrativeCriticOutput;
import storymagine.redacteur.coeur.domaine.orchestrator.EngineConfig;
import storymagine.redacteur.coeur.domaine.orchestrator.GenerationConfig;
import storymagine.redacteur.coeur.domaine.scenario.Chapter;
import storymagine.redacteur.coeur.domaine.scenario.Scenario;
import storymagine.redacteur.coeur.domaine.story.Story;
import storymagine.redacteur.coeur.domaine.story.WrittenChapter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Orchestrates the planning phase for one chapter:
 * ChapterPlanner → critics (narrative + coherence + goal) → retry if any critic has a remark.
 * Retry rule: any problem (AMELIORATION, DEFAUT_SIGNIFICATIF or DEFAUT_MAJEUR) triggers a new pass.
 * Best plan (highest average score across all passes) is retained.
 */
public class PlanWorkflow {

    private final ChapterPlannerStep      chapterPlannerStep;
    private final PlanNarrativeCriticStep planNarrativeCriticStep;
    private final PlanCoherenceCriticStep planCoherenceCriticStep;
    private final GoalPlanCheckerStep     goalPlanCheckerStep;
    private final EngineConfig            engineConfig;
    private final LogPort                 log;

    public PlanWorkflow(ChapterPlannerStep chapterPlannerStep,
                        PlanNarrativeCriticStep planNarrativeCriticStep,
                        PlanCoherenceCriticStep planCoherenceCriticStep,
                        GoalPlanCheckerStep goalPlanCheckerStep,
                        EngineConfig engineConfig,
                        LogPort log) {
        this.chapterPlannerStep      = chapterPlannerStep;
        this.planNarrativeCriticStep = planNarrativeCriticStep;
        this.planCoherenceCriticStep = planCoherenceCriticStep;
        this.goalPlanCheckerStep     = goalPlanCheckerStep;
        this.engineConfig            = engineConfig;
        this.log                     = log;
    }

    /** Plans the current chapter in Story. Mutates Story via WrittenChapter.setPlan/setCoherence. */
    public void run(Scenario scenario, Chapter chapter, Story story, GenerationConfig config) {
        WrittenChapter wc          = story.currentChapter().orElseThrow();
        int            maxAttempts = 1 + engineConfig.planMaxRetry();

        String       bestPlan          = null;
        List<String> bestSequencePlans = List.of();
        double       bestScore         = -1.0;
        int          bestAttempt       = 1;
        int          finalAttempt      = 0;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            finalAttempt = attempt + 1;
            log.phaseHeader("PLAN", "tentative " + (attempt + 1) + "/" + maxAttempts);

            long t0      = System.nanoTime();
            var  planOut = chapterPlannerStep.run(scenario, chapter, story, config.jsonMode());
            wc.setPlan(planOut.fullPlan());
            log.step("ChapterPlanner", ms(t0), null);

            if (!config.mode().runsPlanCritics()) {
                bestPlan          = planOut.fullPlan();
                bestSequencePlans = planOut.sequencePlans();
                break;
            }

            t0 = System.nanoTime();
            PlanNarrativeCriticOutput narrative = planNarrativeCriticStep.run(scenario, chapter, story);
            long narrMs = ms(t0);

            t0 = System.nanoTime();
            PlanCoherenceCriticOutput coherence = planCoherenceCriticStep.run(scenario, chapter, story);
            long cohMs = ms(t0);

            t0 = System.nanoTime();
            GoalPlanCheckerOutput goal = goalPlanCheckerStep.run(scenario, chapter, story);
            long goalMs = ms(t0);

            if (goal.score() == 10 && !goal.problems().isEmpty()) {
                log.warn("GoalPlanChecker score 10 mais PROBLEME non vide : " + goal.problems());
                goal = new GoalPlanCheckerOutput(List.of(), goal.score());
            }

            log.critic("PlanNarrativeCritic", narrative.score(), narrMs, narrative.problems());
            log.critic("PlanCoherenceCritic", coherence.score(), cohMs,  coherence.problems());
            log.critic("GoalPlanChecker",     goal.score(),      goalMs, goal.problems());

            double  avg           = (narrative.score() + coherence.score() + goal.score()) / 3.0;
            boolean passed        = avg >= 10.0;
            boolean isLastAttempt = attempt == maxAttempts - 1;

            if (avg > bestScore) {
                bestScore         = avg;
                bestPlan          = planOut.fullPlan();
                bestSequencePlans = planOut.sequencePlans();
                bestAttempt       = attempt + 1;
            }

            String hint = (!passed && !isLastAttempt) ? (attempt + 2) + "/" + maxAttempts : null;
            log.scoresSummary(avg, passed, hint);

            if (passed || isLastAttempt) break;

            wc.setCoherence(mergeFeedback(narrative.problems(), coherence.problems(), goal.problems()));
        }

        // Restore the best plan found across all passes
        if (bestPlan != null) {
            wc.setPlan(bestPlan);
            wc.setSequencePlans(bestSequencePlans);
        }
        if (finalAttempt > 1) {
            log.planRetained(bestAttempt, finalAttempt, bestScore);
        }
        log.chapterPlan(chapter.title(), wc.plan());
    }

    private static String mergeFeedback(List<String> p1, List<String> p2, List<String> p3) {
        List<String> all = new ArrayList<>();
        all.addAll(p1);
        all.addAll(p2);
        all.addAll(p3);
        return all.stream().map(p -> "- " + p).collect(Collectors.joining("\n"));
    }

    private static long ms(long nanoStart) {
        return (System.nanoTime() - nanoStart) / 1_000_000L;
    }
}
