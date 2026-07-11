package storymagine.redacteur.coeur.domaine.orchestrator.plan;

import storymagine.redacteur.coeur.domaine.agent.plan.continuitycritic.PlanContinuityCritic;
import storymagine.redacteur.coeur.domaine.agent.plan.continuitycritic.PlanContinuityCriticInput;
import storymagine.redacteur.coeur.domaine.agent.plan.continuitycritic.PlanContinuityCriticOutput;
import storymagine.redacteur.coeur.domaine.scenario.Chapter;
import storymagine.redacteur.coeur.domaine.scenario.Scenario;
import storymagine.redacteur.coeur.domaine.story.Story;
import storymagine.redacteur.coeur.domaine.story.WrittenChapter;

import java.util.List;
import java.util.Optional;

/** Activates PlanContinuityCritic against the current chapter plan in Story. */
public class PlanContinuityCriticStep {

    private final PlanContinuityCritic agent;

    public PlanContinuityCriticStep(PlanContinuityCritic agent) {
        this.agent = agent;
    }

    /** Empty when the current chapter is the book's first — nothing earlier to compare against. */
    public Optional<PlanContinuityCriticOutput> run(Scenario scenario, Chapter chapter, Story story) {
        String plan            = story.currentChapter().orElseThrow().plan();
        String previousPlans   = previousChaptersPlansBlock(scenario.chapters(), story.chapters(), chapter);
        if (previousPlans.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(agent.call(new PlanContinuityCriticInput(
                plan,
                chapter.description(),
                chapter.goal(),
                previousPlans,
                scenario.config().language()
        )));
    }

    /** Concatenates the plans of every chapter before the current one, in book order. */
    private static String previousChaptersPlansBlock(List<Chapter> allChapters,
                                                       List<WrittenChapter> written,
                                                       Chapter current) {
        int index = allChapters.indexOf(current);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < index; i++) {
            String plan = written.get(i).plan();
            if (plan == null || plan.isBlank()) continue;
            if (sb.length() > 0) sb.append("\n\n");
            sb.append("Chapitre ").append(allChapters.get(i).title()).append(" :\n").append(plan);
        }
        return sb.toString();
    }
}
