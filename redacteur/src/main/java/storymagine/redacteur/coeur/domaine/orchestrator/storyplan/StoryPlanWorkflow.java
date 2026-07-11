package storymagine.redacteur.coeur.domaine.orchestrator.storyplan;

import storymagine.commun.coeur.ports.LogPort;
import storymagine.redacteur.coeur.domaine.orchestrator.GenerationConfig;
import storymagine.redacteur.coeur.domaine.orchestrator.common.ScenarioFormatters;
import storymagine.redacteur.coeur.domaine.orchestrator.plan.ChapterPlanWorkflow;
import storymagine.redacteur.coeur.domaine.scenario.Chapter;
import storymagine.redacteur.coeur.domaine.scenario.Scenario;
import storymagine.redacteur.coeur.domaine.story.ChapterId;
import storymagine.redacteur.coeur.domaine.story.Story;
import storymagine.redacteur.coeur.domaine.story.WrittenChapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Orchestrates the planning phase for the whole book:
 * plans every chapter (ChapterPlanWorkflow, unchanged) → global critique on the concatenated
 * plans (StoryFidelityCritic + StoryNarrativeCritic + StoryCausalCritic) → retry ALL chapters if
 * the global critique fails.
 * Each chapter is presented to the critics as its author brief (description + goal) FIRST, then
 * its generated plan — the brief always takes precedence: StoryFidelityCritic checks the plan
 * faithfully realises it, StoryNarrativeCritic/StoryCausalCritic only judge what the plan ADDS
 * beyond it (see their prompts). This avoids the critics second-guessing the author's own
 * narrative choices.
 * Retry rule: average of the three book critics below QualityLevel.bookAverageThreshold(), or any
 * critic below QualityLevel.bookEliminationThreshold(), triggers a full replanning pass — every
 * chapter is replanned (never a targeted subset), receiving the merged global feedback as its
 * "coherence" input so ChapterPlanner treats it as a rewrite (see WrittenChapter.coherence()).
 * Best set of chapter plans (highest average book-critic score across all passes) is retained.
 * Skipped entirely (no book critique, no retry) when QualityLevel.runsBookCritics() is false —
 * chapters are still planned once via ChapterPlanWorkflow.
 */
public class StoryPlanWorkflow {

    private final ChapterPlanWorkflow      chapterPlanWorkflow;
    private final StoryFidelityCriticStep  storyFidelityCriticStep;
    private final StoryNarrativeCriticStep storyNarrativeCriticStep;
    private final StoryCausalCriticStep    storyCausalCriticStep;
    private final LogPort                  log;

    public StoryPlanWorkflow(ChapterPlanWorkflow chapterPlanWorkflow,
                             StoryFidelityCriticStep storyFidelityCriticStep,
                             StoryNarrativeCriticStep storyNarrativeCriticStep,
                             StoryCausalCriticStep storyCausalCriticStep,
                             LogPort log) {
        this.chapterPlanWorkflow      = chapterPlanWorkflow;
        this.storyFidelityCriticStep  = storyFidelityCriticStep;
        this.storyNarrativeCriticStep = storyNarrativeCriticStep;
        this.storyCausalCriticStep    = storyCausalCriticStep;
        this.log                      = log;
    }

    /** Plans every chapter of the scenario into Story, then critiques/retries the whole book plan. */
    public void run(Scenario scenario, Story story, GenerationConfig config) {
        List<Chapter> chapters    = scenario.chapters();
        String        bookGoal    = ScenarioFormatters.bookGoal(scenario);
        int           maxAttempts = 1 + config.qualityLevel().bookMaxRetry();

        Map<ChapterId, PlanSnapshot> bestSnapshot = null;
        double                       bestScore    = -1.0;
        int                          bestAttempt  = 1;
        int                          finalAttempt = 0;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            finalAttempt = attempt + 1;
            log.phaseHeader("PLAN LIVRE", "tentative " + (attempt + 1) + "/" + maxAttempts);

            planEveryChapter(scenario, chapters, story, config, attempt == 0);

            if (!config.qualityLevel().runsBookCritics()) {
                return;
            }

            log.phaseHeader("CRITIQUE LIVRE", null);
            String chaptersBlock = buildChaptersBlock(chapters, story);

            long t0 = System.nanoTime();
            var  fidelity = storyFidelityCriticStep.run(bookGoal, chaptersBlock);
            long fidelityMs = ms(t0);

            t0 = System.nanoTime();
            var narrative = storyNarrativeCriticStep.run(bookGoal, chaptersBlock);
            long narrativeMs = ms(t0);

            t0 = System.nanoTime();
            var causal = storyCausalCriticStep.run(bookGoal, chaptersBlock);
            long causalMs = ms(t0);

            log.critic("StoryFidelityCritic",  fidelity.score(),  fidelityMs,  fidelity.problems());
            log.critic("StoryNarrativeCritic", narrative.score(), narrativeMs, narrative.problems());
            log.critic("StoryCausalCritic",    causal.score(),    causalMs,    causal.problems());

            double  elimination   = config.qualityLevel().bookEliminationThreshold();
            double  minScore      = Math.min(fidelity.score(), Math.min(narrative.score(), causal.score()));
            boolean eliminated    = minScore < elimination;
            double  avg           = (fidelity.score() + narrative.score() + causal.score()) / 3.0;
            double  avgThreshold  = config.qualityLevel().bookAverageThreshold();
            boolean passed        = avg >= avgThreshold && !eliminated;
            boolean isLastAttempt = attempt == maxAttempts - 1;

            if (avg >= avgThreshold && eliminated)
                log.warn(String.format("StoryPlanWorkflow : note eliminatoire franchie (seuil %.1f) — relance forcee malgre une moyenne suffisante", elimination));

            if (avg > bestScore) {
                bestScore    = avg;
                bestSnapshot = snapshot(story);
                bestAttempt  = attempt + 1;
            }

            String hint = (!passed && !isLastAttempt) ? "PLAN LIVRE " + (attempt + 2) + "/" + maxAttempts : null;
            log.scoresSummary(avg, avgThreshold, minScore, elimination, passed, hint);

            if (passed || isLastAttempt) break;

            String feedback = mergeFeedback(fidelity.problems(), narrative.problems(), causal.problems());
            for (Chapter chapter : chapters) {
                story.activateChapter(new ChapterId(chapter.title())).setCoherence(feedback);
            }
        }

        if (bestSnapshot != null) {
            restore(story, bestSnapshot);
        }
        if (finalAttempt > 1) {
            log.planRetained(bestAttempt, finalAttempt, bestScore);
        }
    }

    private void planEveryChapter(Scenario scenario, List<Chapter> chapters, Story story,
                                  GenerationConfig config, boolean firstAttempt) {
        for (int i = 0; i < chapters.size(); i++) {
            Chapter   chapter = chapters.get(i);
            ChapterId id      = new ChapterId(chapter.title());
            log.phaseHeader("PLAN chapitre " + (i + 1) + "/" + chapters.size(), chapter.title());

            if (firstAttempt) {
                story.startChapter(id);
            } else {
                story.activateChapter(id);
            }
            chapterPlanWorkflow.run(scenario, chapter, story, config);
        }
    }

    /** Per chapter: author brief (description + goal) FIRST, then the generated plan. */
    private static String buildChaptersBlock(List<Chapter> chapters, Story story) {
        List<WrittenChapter> written = story.chapters();
        StringBuilder        sb      = new StringBuilder();
        for (int i = 0; i < chapters.size(); i++) {
            Chapter        chapter = chapters.get(i);
            WrittenChapter wc      = written.get(i);
            if (sb.length() > 0) sb.append("\n\n");
            sb.append("Chapitre ").append(chapter.title()).append("\n")
              .append("Consigne : ").append(chapter.description()).append("\n")
              .append("Objectif du chapitre : ").append(chapter.goal()).append("\n")
              .append("Plan genere :\n").append(wc.plan());
        }
        return sb.toString();
    }

    private static Map<ChapterId, PlanSnapshot> snapshot(Story story) {
        Map<ChapterId, PlanSnapshot> map = new HashMap<>();
        for (WrittenChapter c : story.chapters()) {
            map.put(c.id(), new PlanSnapshot(c.plan(), c.sequencePlans()));
        }
        return map;
    }

    private static void restore(Story story, Map<ChapterId, PlanSnapshot> snapshot) {
        for (WrittenChapter c : story.chapters()) {
            PlanSnapshot s = snapshot.get(c.id());
            if (s != null) {
                c.setPlan(s.plan());
                c.setSequencePlans(s.sequencePlans());
            }
        }
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

    /** Per-chapter plan state captured at one book-planning attempt, for best-attempt restore. */
    private record PlanSnapshot(String plan, List<String> sequencePlans) {}
}
