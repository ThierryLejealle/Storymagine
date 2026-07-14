package storymagine.redacteur.coeur.domaine.orchestrator.plan;

import storymagine.commun.coeur.ports.LogPort;
import storymagine.redacteur.coeur.domaine.agent.plan.goalcritic.PlanGoalCriticOutput;
import storymagine.redacteur.coeur.domaine.agent.plan.factscritic.PlanFactsCriticOutput;
import storymagine.redacteur.coeur.domaine.agent.plan.coherencecritic.PlanCoherenceCriticOutput;
import storymagine.redacteur.coeur.domaine.agent.plan.continuitycritic.PlanContinuityCriticOutput;
import storymagine.redacteur.coeur.domaine.agent.plan.dramacritic.PlanDramaCriticOutput;
import storymagine.redacteur.coeur.domaine.orchestrator.GenerationConfig;
import storymagine.redacteur.coeur.domaine.scenario.Chapter;
import storymagine.redacteur.coeur.domaine.scenario.Scenario;
import storymagine.redacteur.coeur.domaine.story.Story;
import storymagine.redacteur.coeur.domaine.story.WrittenChapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Orchestrates the planning phase for one chapter:
 * ChapterPlanner → 5 orthogonal critics (goal, facts, coherence, continuity, drama) → retry if
 * any critic has a remark.
 * - A PlanGoalCritic     : fidelity to the author's instruction (chapter goal/description + each
 *                          sequence directive) — beat by beat, literal.
 * - B PlanFactsCritic    : respect of already-established facts (sheets, checks, entity state).
 * - C PlanCoherenceCritic: internal coherence of the plan (no external grounding needed).
 * - D PlanContinuityCritic: narrative continuity with the plans of earlier chapters of this book.
 *                          Skipped for the book's first chapter (nothing earlier to compare
 *                          against) — a skipped critic is not logged and does not enter the
 *                          average/elimination score, so it never dilutes the others.
 * - E PlanDramaCritic    : dramaturgical effort, relative to the intensity the instruction calls for.
 * B/C/D/E all apply the same "consigne fait foi" primacy rule — only A judges fidelity itself,
 * so it needs no such exemption. See PlanGoalCritic.md for the full design rationale.
 * Retry rule: average score below QualityLevel.planAverageThreshold() triggers a new pass ;
 * also retries if any individual critic score falls below QualityLevel.planEliminationThreshold(),
 * even when the average would otherwise pass.
 * FULL only, first draft only (QualityLevel.planStrictFirstAttempt()): a stricter rule applies
 * on top — any remark at all (even a single AMELIORATION, from any critic that ran) forces one
 * retry, even when average/elimination both pass. Never applies past the first attempt, so it
 * can only add at most one extra pass.
 * Best plan (highest average score across all passes) is retained.
 * Called once per chapter by StoryPlanWorkflow, which plans the whole book.
 */
public class ChapterPlanWorkflow {

    private final ChapterPlannerStep        chapterPlannerStep;
    private final PlanGoalCriticStep        planGoalCriticStep;
    private final PlanFactsCriticStep       planFactsCriticStep;
    private final PlanCoherenceCriticStep   planCoherenceCriticStep;
    private final PlanContinuityCriticStep  planContinuityCriticStep;
    private final PlanDramaCriticStep       planDramaCriticStep;
    private final LogPort                   log;

    public ChapterPlanWorkflow(ChapterPlannerStep chapterPlannerStep,
                        PlanGoalCriticStep planGoalCriticStep,
                        PlanFactsCriticStep planFactsCriticStep,
                        PlanCoherenceCriticStep planCoherenceCriticStep,
                        PlanContinuityCriticStep planContinuityCriticStep,
                        PlanDramaCriticStep planDramaCriticStep,
                        LogPort log) {
        this.chapterPlannerStep       = chapterPlannerStep;
        this.planGoalCriticStep       = planGoalCriticStep;
        this.planFactsCriticStep      = planFactsCriticStep;
        this.planCoherenceCriticStep  = planCoherenceCriticStep;
        this.planContinuityCriticStep = planContinuityCriticStep;
        this.planDramaCriticStep      = planDramaCriticStep;
        this.log                      = log;
    }

    /** Plans the current chapter in Story. Mutates Story via WrittenChapter.setPlan/setCoherence. */
    public void run(Scenario scenario, Chapter chapter, Story story, GenerationConfig config) {
        WrittenChapter wc          = story.currentChapter().orElseThrow();
        int            maxAttempts = 1 + config.qualityLevel().planMaxRetry();

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

            if (!config.qualityLevel().runsPlanCritics()) {
                bestPlan          = planOut.fullPlan();
                bestSequencePlans = planOut.sequencePlans();
                break;
            }

            t0 = System.nanoTime();
            PlanGoalCriticOutput goal = planGoalCriticStep.run(scenario, chapter, story);
            long goalMs = ms(t0);

            t0 = System.nanoTime();
            PlanFactsCriticOutput facts = planFactsCriticStep.run(scenario, chapter, story);
            long factsMs = ms(t0);

            t0 = System.nanoTime();
            PlanCoherenceCriticOutput coherence = planCoherenceCriticStep.run(scenario, chapter, story);
            long cohMs = ms(t0);

            t0 = System.nanoTime();
            Optional<PlanContinuityCriticOutput> continuity = planContinuityCriticStep.run(scenario, chapter, story);
            long contMs = ms(t0);

            t0 = System.nanoTime();
            PlanDramaCriticOutput drama = planDramaCriticStep.run(scenario, chapter, story);
            long dramaMs = ms(t0);

            if (goal.score() == 10 && !goal.problems().isEmpty()) {
                log.warn("PlanGoalCritic score 10 mais PROBLEME non vide : " + goal.problems());
                goal = new PlanGoalCriticOutput(List.of(), goal.score());
            }

            log.critic("PlanGoalCritic",       goal.score(),       goalMs,  goal.problems());
            log.critic("PlanFactsCritic",      facts.score(),      factsMs, facts.problems());
            log.critic("PlanCoherenceCritic",  coherence.score(),  cohMs,   coherence.problems());
            continuity.ifPresent(c -> log.critic("PlanContinuityCritic", c.score(), contMs, c.problems()));
            log.critic("PlanDramaCritic",      drama.score(),      dramaMs, drama.problems());

            // Only critics that actually ran contribute to the average/elimination score —
            // a skipped critic (nothing to compare against) must not dilute the others.
            List<Double> scores = new ArrayList<>(List.of(goal.score(), facts.score(), coherence.score(), drama.score()));
            continuity.ifPresent(c -> scores.add(c.score()));

            double  elimination   = config.qualityLevel().planEliminationThreshold();
            double  minScore      = scores.stream().mapToDouble(Double::doubleValue).min().orElseThrow();
            boolean eliminated    = minScore < elimination;
            double  avg           = scores.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
            double  avgThreshold  = config.qualityLevel().planAverageThreshold();
            boolean scorePassed   = avg >= avgThreshold && !eliminated;
            boolean isLastAttempt = attempt == maxAttempts - 1;

            if (avg >= avgThreshold && eliminated)
                log.warn(String.format("ChapterPlanWorkflow : note eliminatoire franchie (seuil %.1f) — relance forcee malgre une moyenne suffisante", elimination));

            // FULL only, first draft only: any remark at all (even a single AMELIORATION)
            // forces one retry, stricter than the usual average/elimination gate. Later
            // retries fall back to the normal rule — this never loops more than once for it.
            boolean hasAnyProblem = !goal.problems().isEmpty() || !facts.problems().isEmpty()
                    || !coherence.problems().isEmpty()
                    || continuity.map(c -> !c.problems().isEmpty()).orElse(false)
                    || !drama.problems().isEmpty();
            // Only "forced" in the meaningful sense when the scores themselves passed — a score
            // failure combined with remaining remarks is just a normal (red) score retry.
            boolean forcedRetry = config.qualityLevel().planStrictFirstAttempt()
                    && attempt == 0 && hasAnyProblem && scorePassed;

            if (forcedRetry)
                log.info("relance parce qu'il reste au moins une amelioration");

            boolean willRetry = !scorePassed || forcedRetry;

            if (avg > bestScore) {
                bestScore         = avg;
                bestPlan          = planOut.fullPlan();
                bestSequencePlans = planOut.sequencePlans();
                bestAttempt       = attempt + 1;
            }

            String hint = (willRetry && !isLastAttempt) ? "PLAN " + (attempt + 2) + "/" + maxAttempts : null;
            log.scoresSummary(avg, avgThreshold, minScore, elimination, scorePassed, forcedRetry, hint);

            if (!willRetry || isLastAttempt) break;

            wc.setCoherence(mergeFeedback(goal.problems(), facts.problems(), coherence.problems(),
                    continuity.map(PlanContinuityCriticOutput::problems).orElse(List.of()), drama.problems()));
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

    private static String mergeFeedback(List<String> p1, List<String> p2, List<String> p3,
                                         List<String> p4, List<String> p5) {
        List<String> all = new ArrayList<>();
        all.addAll(p1);
        all.addAll(p2);
        all.addAll(p3);
        all.addAll(p4);
        all.addAll(p5);
        return all.stream().map(p -> "- " + p).collect(Collectors.joining("\n"));
    }

    private static long ms(long nanoStart) {
        return (System.nanoTime() - nanoStart) / 1_000_000L;
    }
}
